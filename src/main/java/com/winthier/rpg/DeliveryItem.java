package com.winthier.rpg;

import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.item.TickableItem;
import com.winthier.custom.item.UncraftableItem;
import com.winthier.custom.util.Dirty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

@RequiredArgsConstructor
public final class DeliveryItem implements CustomItem, UncraftableItem, TickableItem {
    private final RPGPlugin plugin;
    public static final String CUSTOM_ID = "rpg:delivery";

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.WRITTEN_BOOK);
    }

    Vec2 findNewDot(Block pb, ItemStack item, RPGWorld.NPC npc) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        Vec2 v = new Vec2(pb.getX(), pb.getZ());
        int dx = npc.home.x - v.x;
        int dz = npc.home.z - v.y;
        int dist = Math.max(Math.abs(dx), Math.abs(dz));
        if (dist < 128) return new Vec2(npc.home.x, npc.home.z);
        boolean b = plugin.getRandom().nextInt(Math.abs(dx) + Math.abs(dz)) < Math.abs(dx);
        int relX, relZ;
        int rel = plugin.getRandom().nextInt(16);
        if (b) {
            relX = dx > 0 ? 64 : -64;
            relZ = dz > 0 ? rel : -rel;
        } else {
            relX = dx > 0 ? rel : -rel;
            relZ = dz > 0 ? 64 : -64;
        }
        return v.relative(relX, relZ);
    }

    @Override
    public void onTick(ItemContext context, int ticks) {
        if (ticks % 20 != 0) return;
        Player player = context.getPlayer();
        if (player == null) return;
        if (plugin.getRPGWorld(player.getWorld()) == null) return;
        ItemStack item = context.getItemStack();
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        if (config.getLong("world_timestamp") != plugin.getRPGWorld().getTimestamp()) {
            item.setAmount(0);
            return;
        }
        if (!player.getUniqueId().toString().equals(config.getString("owner"))) {
            return;
        }
        int townId = config.getInt("town_id");
        RPGWorld.Town town = plugin.getRPGWorld().findTown(townId);
        if (town == null) return;
        int npcId = config.getInt("npc_id");
        RPGWorld.NPC npc = plugin.getRPGWorld().findNPC(townId, npcId);
        Block pb = player.getLocation().getBlock();
        if (npc == null) return;
        List<Integer> listDot = config.getIntList("dot");
        Vec2 dot;
        if (town.area.contains(pb.getX(), pb.getZ())) {
            dot = new Vec2(npc.home.x, npc.home.z);
            config.setIntList("dot", dot.serialize());
        } else if (listDot == null || listDot.size() != 2) {
            dot = findNewDot(pb, item, npc);
            if (dot == null) return;
            config.setIntList("dot", dot.serialize());
        } else {
            dot = new Vec2(listDot);
            int dx = dot.x - pb.getX();
            int dz = dot.y - pb.getZ();
            int dist = Math.max(Math.abs(dx), Math.abs(dz));
            if (dist <= 8) {
                dot = findNewDot(pb, item, npc);
                if (dot == null) return;
                config.setIntList("dot", dot.serialize());
            }
        }
        MetadataValue meta = null;
        for (MetadataValue m: player.getMetadata("MiniMapCursors")) {
            if (m.getOwningPlugin() == plugin) {
                meta = m;
                break;
            }
        }
        List<Map> list;
        if (meta == null) {
            list = new ArrayList<>();
            meta = new FixedMetadataValue(plugin, list);
            player.setMetadata("MiniMapCursors", meta);
        } else {
            list = (List<Map>)meta.value();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("block", player.getWorld().getBlockAt(dot.x, 65, dot.y));
        map.put("type", MapCursor.Type.SMALL_WHITE_CIRCLE);
        list.add(map);
    }
}
