package com.github.aitooor.quickvoid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final QuickVoidPlugin plugin;

    public CommandHandler(QuickVoidPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            return false;
        }
        if (!args[0].equalsIgnoreCase("create")) {
            return false;
        }
        if (args.length < 2 || args.length > 3) {
            return false;
        }

        String worldName = args[1];
        if (plugin.isWorldNameInvalid(worldName)) {
            sender.sendMessage(Component.text("Invalid world name. Use only letters, numbers, _ and - (max 32).")
                    .color(NamedTextColor.RED));
            return true;
        }

        boolean useMultiverse = false;
        if (args.length == 3) {
            if (!"--mv".equalsIgnoreCase(args[2])) {
                sender.sendMessage(Component.text("Unknown flag. Use only --mv")
                        .color(NamedTextColor.RED));
                return true;
            }
            useMultiverse = true;
            if (plugin.isMultiverseMissing()) {
                sender.sendMessage(Component.text("Multiverse-Core is not loaded. Remove --mv or install Multiverse-Core.")
                        .color(NamedTextColor.RED));
                return true;
            }
        }

        sender.sendMessage("Creating a void world named '" + worldName + "'"
                + (useMultiverse ? " using Multiverse..." : " using native mode..."));

        plugin.createVoidWorldAsync(worldName, useMultiverse, success -> {
            if (success) {
                sender.sendMessage(Component.text("A world has been successfully created")
                        .color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to create a void world").color(NamedTextColor.RED));
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String @NotNull [] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("create");
            return list;
        }
        if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            return List.of("--mv");
        }
        return Collections.emptyList();
    }

}
