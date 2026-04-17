package com.github.aitooor.quickvoid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;

public class QuickVoidPlugin extends JavaPlugin {

    private static QuickVoidPlugin instance;
    private MultiverseCoreApi coreApi;

    @Override
    public void onEnable() {
        instance = this;
        try {
            coreApi = MultiverseCoreApi.get();
        } catch (IllegalStateException e) {
            getLogger().severe("Multiverse-Core API is not loaded! Make sure Multiverse-Core is installed and loaded before this plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        CommandHandler cmd = new CommandHandler(instance);
        var pluginCmd = getServer().getPluginCommand("quickvoid");
        if (pluginCmd != null) {
            pluginCmd.setExecutor(cmd);
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

    /**
     * Returns the Multiverse-Core API instance.
     *
     * @return the Multiverse-Core API
     */
    @SuppressWarnings("unused")
    public MultiverseCoreApi getCoreApi() {
        return coreApi;
    }

    /**
     * Creates a new void world using a preset level.dat
     * and Multiverse-Core.
     * 
     * @param name a desired world name
     * @return <code>true</code> if a world is created successfully,
     * otherwise, returns <code>false</code>
     */
    public boolean createVoidWorld(String name) {
        saveResource("level.dat", false);
        File container = Bukkit.getWorldContainer();
        File oldLevelFile = new File(getDataFolder(), "level.dat");
        File newLevelDir = new File(container, name);
        if (!newLevelDir.isDirectory()) {
            boolean created = newLevelDir.mkdir();
            if (!created) {
                getLogger().warning("Could not create directory for world " + name);
            }
        }
        File newLevelFile = new File(newLevelDir, "level.dat");
        try {
            Files.move(oldLevelFile.toPath(), newLevelFile.toPath());
        } catch (IOException e) {
            getLogger().severe("Failed to move level.dat: " + e.getMessage());
            return false;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        WorldManager worldManager = coreApi.getWorldManager();

        worldManager.loadWorld(name)
                .onSuccess(ignored -> {
                    success.set(true);
                    getLogger().info("World '" + name + "' loaded successfully!");

                    // Place bedrock under spawn location
                    org.bukkit.World world = Bukkit.getWorld(name);
                    if (world != null) {
                        Block ground = world.getSpawnLocation().clone()
                                .subtract(0.0D, 1.0D, 0.0D).getBlock();
                        if (ground.getType() == Material.AIR) {
                            for (int x = -1; x < 2; x++) {
                                for (int z = -1; z < 2; z++) {
                                    world.getBlockAt(ground.getX() + x, ground.getY(), ground.getZ() + z)
                                            .setType(Material.BEDROCK);
                                }
                            }
                        }
                    }
                    latch.countDown();
                })
                .onFailure(reason -> {
                    getLogger().severe("Failed to load void world '" + name + "': " + reason);
                    success.set(false);
                    latch.countDown();
                });

        // Wait for the operation to complete (with timeout)
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                getLogger().warning("World loading timed out for '" + name + "'");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getLogger().severe("World loading was interrupted for '" + name + "'");
            return false;
        }

        return success.get();
    }

}
