package com.winthier.rpg;

import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.UncraftableItem;
import com.winthier.custom.item.UpdatableItem;
import com.winthier.custom.util.Dirty;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@RequiredArgsConstructor
public final class QuestTokenItem implements CustomItem, UncraftableItem {
    public static final String CUSTOM_ID = "rpg:quest_token";
    private final RPGPlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.GOLD_NUGGET, amount);
    }

    static void save(ItemStack item, RPGWorld world, String tokenName) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        config.setString("token", tokenName);
        config.setLong("timestamp", world.getTimestamp());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + tokenName);
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    static String getToken(ItemStack item) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        return config.getString("token");
    }
}
