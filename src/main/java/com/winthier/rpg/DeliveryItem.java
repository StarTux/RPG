package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.item.TickableItem;
import com.winthier.custom.item.UncraftableItem;
import com.winthier.custom.util.Dirty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

@RequiredArgsConstructor
public final class DeliveryItem implements CustomItem, UncraftableItem, TickableItem {
    private final RPGPlugin plugin;
    public static final String CUSTOM_ID = "rpg:delivery";
    static final String KEY_SENDER = "sender";
    static final String KEY_RECIPIENT = "recipient";
    static final String KEY_USED = "used";
    static final String KEY_TIMESTAMP = "timestamp";
    static final String KEY_OWNER = "owner";
    static final String KEY_DOT = "dot";
    private boolean interactNPCFlag;

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
        int rel = 1 + plugin.getRandom().nextInt(16);
        if (b) {
            relX = dx > 0 ? 96 : -96;
            relZ = dz > 0 ? rel : -rel;
        } else {
            relX = dx > 0 ? rel : -rel;
            relZ = dz > 0 ? 96 : -96;
        }
        return v.relative(relX, relZ);
    }

    @Override
    public void onTick(ItemContext context, int ticks) {
        if (ticks % 20 != 0) return;
        Player player = context.getPlayer();
        if (player == null) return;
        if (plugin.getRPGWorld() == null) return;
        ItemStack item = context.getItemStack();
        if (item.getAmount() != 1) return;
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        if (!config.isSet(KEY_TIMESTAMP) || !config.isSet(KEY_OWNER)) {
            plugin.getRPGWorld().updateDeliveryItem(item, player);
            return;
        }
        if (plugin.getRPGWorld(player.getWorld()) == null) return;
        if (config.getLong(KEY_TIMESTAMP) != plugin.getRPGWorld().getTimestamp()) {
            item.setAmount(0);
            return;
        }
        if (!player.getUniqueId().toString().equals(config.getString(KEY_OWNER))) return;
        if (!config.isSet(KEY_RECIPIENT)) return;
        Vec2 vecRecipient = new Vec2(config.getIntList(KEY_RECIPIENT));
        RPGWorld.Town town = plugin.getRPGWorld().findTown(vecRecipient.x);
        if (town == null) return;
        RPGWorld.NPC npc = plugin.getRPGWorld().findNPC(vecRecipient.x, vecRecipient.y);
        Block pb = player.getLocation().getBlock();
        if (npc == null) return;
        List<Integer> listDot = config.getIntList(KEY_DOT);
        Vec2 dot = config.isSet(KEY_DOT) ? new Vec2(config.getIntList(KEY_DOT)) : null;
        Vec2 vecPlayer = new Vec2(pb.getX(), pb.getZ());
        Vec2 vecTarget = new Vec2(npc.home.x, npc.home.z);
        if (town.area.contains(pb.getX(), pb.getZ())) {
            if (!vecTarget.equals(dot)) {
                dot = vecTarget;
                config.setIntList("dot", dot.serialize());
            }
        } else if (dot == null || dot.distanceSquared(vecTarget) > vecPlayer.distanceSquared(vecTarget)) {
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

    static Vec2 getRecipient(ItemStack item) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        if (config == null) return new Vec2(-1, -1);
        if (!config.isSet(KEY_RECIPIENT)) return new Vec2(-1, -1);
        return new Vec2(config.getIntList(KEY_RECIPIENT));
    }

    static Vec2 getSender(ItemStack item) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        if (config == null) return new Vec2(-1, -1);
        if (!config.isSet(KEY_SENDER)) return new Vec2(-1, -1);
        return new Vec2(config.getIntList(KEY_SENDER));
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, ItemContext context) {
        if (CustomPlugin.getInstance().getEntityManager().getCustomEntity(event.getRightClicked()) instanceof NPCEntity) {
            interactNPCFlag = true;
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event, ItemContext context) {
        if (interactNPCFlag) {
            interactNPCFlag = false;
            event.setUseItemInHand(Event.Result.DENY);
        }
    }
}
