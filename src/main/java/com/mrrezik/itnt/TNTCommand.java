package com.mrrezik.itnt.commands;

import com.mrrezik.itnt.itnt;
import com.mrrezik.itnt.managers.ConfigManager;
import com.mrrezik.itnt.objects.CustomTNT;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Обрабатывает все команды плагина /itnt.
 */
public class TNTCommand implements CommandExecutor, TabCompleter {

    private final itnt plugin;
    private final ConfigManager configManager;

    public TNTCommand(itnt plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("itnt.reload")) {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(configManager.getMessage("reload"));
                return true;

            case "give":
                if (!sender.hasPermission("itnt.give")) {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                    return true;
                }
                return handleGive(sender, args);

            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Логика подкоманды /itnt give
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        // /itnt give <player> <id> [amount]
        if (args.length < 3) {
            sender.sendMessage(configManager.getRawMessage("help-give")); // Показываем raw, т.к. префикс не нужен
            return true;
        }

        // 1. Получаем игрока
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(configManager.getMessage("player-not-found").replace("%player%", args[1]));
            return true;
        }

        // 2. Получаем тип ТНТ (по ID или Алиасу)
        String tntId = args[2];
        CustomTNT tntConfig = configManager.getTNTType(tntId);
        if (tntConfig == null) {
            sender.sendMessage(configManager.getMessage("tnt-not-found").replace("%tnt%", tntId));
            return true;
        }

        // 3. Получаем количество
        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) {
                    sender.sendMessage(configManager.getMessage("invalid-amount"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(configManager.getMessage("invalid-amount"));
                return true;
            }
        }

        // 4. Выдаем предмет
        ItemStack item = configManager.getTNTItem(tntConfig, amount);
        target.getInventory().addItem(item);

        // 5. Сообщения
        String tntName = tntConfig.getDisplayName(); // Имя уже с цветами

        // Отправителю
        sender.sendMessage(configManager.getMessage("give-success")
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf(amount))
                .replace("%tnt_name%", tntName));

        // Получателю
        target.sendMessage(configManager.getMessage("give-received")
                .replace("%amount%", String.valueOf(amount))
                .replace("%tnt_name%", tntName));

        return true;
    }

    /**
     * Отправляет отправителю список доступных команд.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(configManager.getRawMessage("help-header"));
        if (sender.hasPermission("itnt.give")) {
            sender.sendMessage(configManager.getRawMessage("help-give"));
        }
        if (sender.hasPermission("itnt.reload")) {
            sender.sendMessage(configManager.getRawMessage("help-reload"));
        }
        if (sender.hasPermission("itnt.help")) {
            sender.sendMessage(configManager.getRawMessage("help-help"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String lastArg = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            if (sender.hasPermission("itnt.give")) completions.add("give");
            if (sender.hasPermission("itnt.reload")) completions.add("reload");
            if (sender.hasPermission("itnt.help")) completions.add("help");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Список игроков
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));

        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Список ID и Алиасов ТНТ
            completions.addAll(configManager.getTNTTypeKeys());
        }

        // Фильтрация
        return StringUtil.copyPartialMatches(lastArg, completions, new ArrayList<>());
    }
}