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
        GREETING,
        TOWN_SIGN,
        DISTANT_RELATIONSHIP,
        SYNONYM_DELIVER,
        SYNONYM_DELIVERY,
        SYNONYM_LIVES_IN,
        SYNONYM_A_PLACE_CALLED,
        SYNONYM_SINCERELY,
        SYNONYM_LEGENDARY_ITEM,
        SYNONYM_FINE_ITEM,
        DELIVERY_THANKS,
        QUEST_MINE,
        QUEST_KILL_STOLEN_BABY_PETS,
        QUEST_MINE_SUCCESS,
        QUEST_KILL,
        QUEST_KILL_SUCCESS,
        QUEST_SHEAR,
        QUEST_SHEAR_PROGRESS,
        QUEST_SHEAR_SUCCESS,
        QUEST_BREED,
        QUEST_BREED_PROGRESS,
        QUEST_BREED_SUCCESS,
        QUEST_EXPIRED,
        QUEST_UNWORTHY;
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
            if (message.messages.isEmpty()) plugin.getLogger().warning("Message list empty: " + type);
            Collections.shuffle(message.messages);
            messages.put(type, message);
        }
    }

    String deal(Type type) {
        return messages.get(type).deal();
    }
}
