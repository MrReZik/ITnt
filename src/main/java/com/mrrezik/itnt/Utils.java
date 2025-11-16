package com.mrrezik.itnt.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Утилиты для форматирования строк.
 */
public class Utils {

    // Паттерн для &#RRGGBB
    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    // Паттерн для <#RRGGBB> (MiniMessage)
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    /**
     * Форматирует строку, переводя & коды и HEX-коды (&#RRGGBB и <#RRGGBB>).
     */
    public static String color(String message) {
        if (message == null) {
            return "";
        }

        // 1. Обработка &#RRGGBB
        Matcher matcher1 = HEX_PATTERN_1.matcher(message);
        while (matcher1.find()) {
            message = message.replace(matcher1.group(), ChatColor.of("#" + matcher1.group(1)).toString());
        }

        // 2. Обработка <#RRGGBB>
        Matcher matcher2 = HEX_PATTERN_2.matcher(message);
        while (matcher2.find()) {
            message = message.replace(matcher2.group(), ChatColor.of("#" + matcher2.group(1)).toString());
        }

        // 3. Обработка стандартных & кодов
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Форматирует список строк.
     */
    public static List<String> color(List<String> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(Utils::color).collect(Collectors.toList());
    }
}