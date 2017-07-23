package com.winthier.rpg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONValue;

final class Msg {
    private Msg() { }

    public static String capitalize(String inp) {
        if (inp.isEmpty()) return inp;
        return inp.substring(0, 1).toUpperCase() + inp.substring(1).toLowerCase();
    }

    public static String format(String msg, Object... args) {
        if (msg == null) return "";
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) {
            msg = String.format(msg, args);
        }
        return msg;
    }

    public static void consoleCommand(String cmd, Object... args) {
        if (args.length > 0) cmd = String.format(cmd, args);
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
    }

    public static void sendActionBar(Player player, String msg, Object... args) {
        Object o = button(format(msg, args), null, null, null);
        consoleCommand("minecraft:title %s actionbar %s", player.getName(), JSONValue.toJSONString(o));
    }

    public static void raw(Player player, Object... obj) {
        if (obj.length == 0) return;
        if (obj.length == 1) {
            consoleCommand("minecraft:tellraw %s %s", player.getName(), JSONValue.toJSONString(obj[0]));
        } else {
            consoleCommand("minecraft:tellraw %s %s", player.getName(), JSONValue.toJSONString(Arrays.asList(obj)));
        }
    }

    public static Object button(String chat, String insertion, String tooltip, String command, ChatColor... colors) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", format(chat));
        if (colors != null) {
            for (ChatColor color: colors) {
                if (color.isColor()) {
                    map.put("color", color.name().toLowerCase());
                } else if (color == ChatColor.BOLD) {
                    map.put("bold", "true");
                } else if (color == ChatColor.ITALIC) {
                    map.put("bold", "italic");
                } else if (color == ChatColor.UNDERLINE) {
                    map.put("bold", "underline");
                } else if (color == ChatColor.STRIKETHROUGH) {
                    map.put("bold", "strikethrough");
                }
            }
        }
        if (insertion != null) {
            map.put("insertion", insertion);
        }
        if (command != null) {
            Map<String, Object> clickEvent = new HashMap<>();
            map.put("clickEvent", clickEvent);
            clickEvent.put("action", command.endsWith(" ") ? "suggest_command" : "run_command");
            clickEvent.put("value", command);
        }
        if (tooltip != null) {
            Map<String, Object> hoverEvent = new HashMap<>();
            map.put("hoverEvent", hoverEvent);
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", format(tooltip));
        }
        return map;
    }

    public static Object parseJson(String s) {
        return JSONValue.parse(s);
    }

    public static String toJsonString(Object o) {
        return JSONValue.toJSONString(o);
    }
}
