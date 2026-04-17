package com.github.aitooor.quickvoid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.regex.Pattern;

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
    private MultiverseCoreApi coreApi;
    private boolean multiverseChecked;

    @Override
    public void onEnable() {
        instance = this;
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

    public boolean createVoidWorld(String name, boolean useMultiverse) {
        if (isWorldNameInvalid(name)) {
            getLogger().warning("Invalid world name: " + name);
            return false;
        }
        if (Bukkit.getWorld(name) != null) {
            getLogger().warning("World '" + name + "' already exists");
            return false;
        }
        return useMultiverse ? createVoidWorldWithMultiverse(name) : createVoidWorldNative(name);
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

    private boolean createVoidWorldNative(String name) {
        try {
            prepareLevelDat(name);
        } catch (IOException e) {
            getLogger().severe("Failed to copy level.dat: " + e.getMessage());
            return false;
        }

        World world = Bukkit.createWorld(new WorldCreator(name).environment(World.Environment.NORMAL));
        if (world == null) {
            getLogger().severe("Failed to create native void world '" + name + "'");
            return false;
        }

        placeSpawnPlatform(world);
        return true;
    }

    private boolean createVoidWorldWithMultiverse(String name) {
        if (isMultiverseMissing()) {
            getLogger().warning("Multiverse mode requested but Multiverse-Core is not available");
            return false;
        }

        try {
            prepareLevelDat(name);
        } catch (IOException e) {
            getLogger().severe("Failed to copy level.dat: " + e.getMessage());
            return false;
        }

        WorldManager worldManager = Objects.requireNonNull(coreApi).getWorldManager();
        final boolean[] success = { false };
        worldManager.loadWorld(name)
                .onSuccess(ignored -> success[0] = true)
                .onFailure(reason -> getLogger().severe("Failed to load void world '" + name + "': " + reason));

        World world = Bukkit.getWorld(name);
        if (success[0] && world != null) {
            placeSpawnPlatform(world);
            return true;
        }
        return false;
    }

    private void prepareLevelDat(String name) throws IOException {
        saveResource("level.dat", false);
        File container = Bukkit.getWorldContainer();
        File templateLevelFile = new File(getDataFolder(), "level.dat");
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
}
