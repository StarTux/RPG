package com.winthier.rpg;

import java.io.File;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class Reputations {
    final RPGPlugin plugin;
    private YamlConfiguration config;

    void load() {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "reputation.yml"));
    }

    void save() {
        if (config == null) return;
        try {
            config.save(new File(plugin.getDataFolder(), "reputation.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    int getReputation(Player player, Generator.Flag fraction) {
        if (config == null) load();
        ConfigurationSection section = config.getConfigurationSection(player.getUniqueId().toString());
        if (section == null) return 0;
        return section.getInt(fraction.name(), 0);
    }

    int giveRepurtation(Player player, Generator.Flag fraction, int amount) {
        if (config == null) load();
        ConfigurationSection section = config.getConfigurationSection(player.getUniqueId().toString());
        if (section == null) section = config.createSection(player.getUniqueId().toString());
        int newVal = section.getInt(fraction.name(), 0) + amount;
        section.set(fraction.name(), newVal);
        save();
        return newVal;
    }
}
