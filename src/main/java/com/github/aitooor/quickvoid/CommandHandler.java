package com.github.aitooor.quickvoid;

import java.util.ArrayList;
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
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                sender.sendMessage("Creating a void world named '" + args[1] + "'...");
                if (plugin.createVoidWorld(args[1])) {
                    sender.sendMessage("A world has been successfully created");
                } else {
                    sender.sendMessage(Component.text("Failed to create a void world").color(NamedTextColor.RED));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String @NotNull [] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("create");
            return list;
        } else {
            return null;
        }
    }

}
