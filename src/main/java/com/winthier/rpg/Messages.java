package com.winthier.rpg;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;

@RequiredArgsConstructor
final class Messages {
    private final RPGPlugin plugin;
    private final Map<Type, Message> messages = new HashMap<>();

    enum Type {
        RANDOM,
        GREETING;
    }

    class Message {
        final List<String> messages = new ArrayList<>();
        int index;

        String deal() {
            if (messages.isEmpty()) return "";
            if (index >= messages.size()) {
                index = 0;
            }
            String result = messages.get(index);
            index += 1;
            return result;
        }
    }

    void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("messages.yml")));
        for (Type type: Type.values()) {
            Message message = new Message();
            message.messages.addAll(config.getStringList(type.name().toLowerCase()));
            Collections.shuffle(message.messages);
            messages.put(type, message);
        }
    }

    String deal(Type type) {
        return messages.get(type).deal();
    }
}
