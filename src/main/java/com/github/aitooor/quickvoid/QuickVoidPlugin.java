package com.github.aitooor.quickvoid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;

public class QuickVoidPlugin extends JavaPlugin {

    private static final Pattern WORLD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{1,32}$");
    private static QuickVoidPlugin instance;

    private final Set<String> worldsInProgress = new HashSet<>();
    private MultiverseCoreApi coreApi;
    private boolean multiverseChecked;
    private File templateLevelFile;

    @Override
    public void onEnable() {
        instance = this;
        ensureTemplateLevelDat();

        CommandHandler cmd = new CommandHandler(instance);
        var pluginCmd = getServer().getPluginCommand("quickvoid");
        if (pluginCmd != null) {
            pluginCmd.setExecutor(cmd);
            pluginCmd.setTabCompleter(cmd);
            getLogger().info("QuickVoid plugin enabled successfully!");
        } else {
            getLogger().warning("Could not register quickvoid command!");
        }
    }

    /**
     * Returns a running plugin instance of QuickVoidPlugin.
     * 
     * @return a plugin
     */
    @SuppressWarnings("unused")
    public static QuickVoidPlugin getInstance() {
        return instance;
    }

    public boolean isWorldNameInvalid(String name) {
        return name == null || !WORLD_NAME_PATTERN.matcher(name).matches();
    }

    public boolean isMultiverseMissing() {
        if (multiverseChecked) {
            return coreApi == null;
        }
        multiverseChecked = true;
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") == null) {
            return true;
        }
        try {
            coreApi = MultiverseCoreApi.get();
            return false;
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    public void createVoidWorldAsync(String name, boolean useMultiverse, Consumer<Boolean> callback) {
        if (isWorldNameInvalid(name)) {
            getLogger().warning("Invalid world name: " + name);
            callback.accept(false);
            return;
        }
        if (Bukkit.getWorld(name) != null) {
            getLogger().warning("World '" + name + "' already exists");
            callback.accept(false);
            return;
        }

        synchronized (worldsInProgress) {
            if (!worldsInProgress.add(name.toLowerCase())) {
                getLogger().warning("World creation already in progress for '" + name + "'");
                callback.accept(false);
                return;
            }
        }

        // File operations can run async to minimize impact on server ticks.
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                prepareLevelDat(name);
            } catch (IOException e) {
                getLogger().severe("Failed to copy level.dat: " + e.getMessage());
                finishWorldCreation(name, callback, false);
                return;
            }

            Bukkit.getScheduler().runTask(this, () -> {
                if (useMultiverse) {
                    createVoidWorldWithMultiverse(name, callback);
                } else {
                    boolean success = createVoidWorldNative(name);
                    finishWorldCreation(name, callback, success);
                }
            });
        });
    }

    private boolean createVoidWorldNative(String name) {
        World world = Bukkit.createWorld(new WorldCreator(name).environment(World.Environment.NORMAL));
        if (world == null) {
            getLogger().severe("Failed to create native void world '" + name + "'");
            return false;
        }

        placeSpawnPlatform(world);
        return true;
    }

    private void createVoidWorldWithMultiverse(String name, Consumer<Boolean> callback) {
        if (isMultiverseMissing()) {
            getLogger().warning("Multiverse mode requested but Multiverse-Core is not available");
            finishWorldCreation(name, callback, false);
            return;
        }

        WorldManager worldManager = Objects.requireNonNull(coreApi).getWorldManager();
        worldManager.loadWorld(name)
                .onSuccess(ignored -> {
                    World world = Bukkit.getWorld(name);
                    if (world != null) {
                        placeSpawnPlatform(world);
                        finishWorldCreation(name, callback, true);
                    } else {
                        finishWorldCreation(name, callback, false);
                    }
                })
                .onFailure(reason -> {
                    getLogger().severe("Failed to load void world '" + name + "': " + reason);
                    finishWorldCreation(name, callback, false);
                });
    }

    private void ensureTemplateLevelDat() {
        saveResource("level.dat", false);
        templateLevelFile = new File(getDataFolder(), "level.dat");
        if (!templateLevelFile.isFile()) {
            getLogger().severe("level.dat template is missing in plugin data folder.");
        }
    }

    private void prepareLevelDat(String name) throws IOException {
        if (templateLevelFile == null || !templateLevelFile.isFile()) {
            throw new IOException("Template level.dat is missing");
        }

        File container = Bukkit.getWorldContainer();
        File newLevelDir = new File(container, name);
        if (!newLevelDir.isDirectory()) {
            boolean created = newLevelDir.mkdirs();
            if (!created) {
                throw new IOException("Could not create directory for world " + name);
            }
        }

        File newLevelFile = new File(newLevelDir, "level.dat");
        Files.copy(templateLevelFile.toPath(), newLevelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void placeSpawnPlatform(World world) {
        Block ground = world.getSpawnLocation().clone()
                .subtract(0.0D, 1.0D, 0.0D).getBlock();
        if (ground.getType() != Material.AIR) {
            return;
        }
        for (int x = -1; x < 2; x++) {
            for (int z = -1; z < 2; z++) {
                world.getBlockAt(ground.getX() + x, ground.getY(), ground.getZ() + z)
                        .setType(Material.BEDROCK);
            }
        }
    }

    private void finishWorldCreation(String name, Consumer<Boolean> callback, boolean success) {
        synchronized (worldsInProgress) {
            worldsInProgress.remove(name.toLowerCase());
        }
        callback.accept(success);
    }
}
