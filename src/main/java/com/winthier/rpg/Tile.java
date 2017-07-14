package com.winthier.rpg;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.Material;

final class Tile {
    public final Material mat;
    public final int data;
    private static final Map<Material, Tile[]> cache = new EnumMap<>(Material.class);

    private Tile(Material mat, int data) {
        this.mat = mat;
        this.data = data;
    }

    static Tile of(Material mat, int data) {
        if (!mat.isBlock()) throw new IllegalArgumentException("Material " + mat + " is not a block!");
        Tile[] array = cache.get(mat);
        if (array == null) {
            array = new Tile[256];
            cache.put(mat, array);
        }
        data &= 0xff;
        Tile tile = array[data];
        if (tile == null) {
            tile = new Tile(mat, data);
            array[data] = tile;
        }
        return tile;
    }

    static Tile of(Material mat) {
        return of(mat, 0);
    }

    static final Tile AIR = of(Material.AIR);
    static final Tile OAK_LOG = of(Material.LOG);
    static final Tile SPRUCE_LOG = of(Material.LOG, 1);
    static final Tile BIRCH_LOG = of(Material.LOG, 2);
    static final Tile JUNGLE_LOG = of(Material.LOG, 3);
    static final Tile ACACIA_LOG = of(Material.LOG_2);
    static final Tile DARK_OAK_LOG = of(Material.LOG_2, 1);
    static final Tile COBBLESTONE = of(Material.COBBLESTONE);
    static final Tile COBBLESTONE_WALL = of(Material.COBBLE_WALL);
    static final Tile OAK_WOOD_SLAB = of(Material.WOOD_STEP);
    static final Tile SPRUCE_WOOD_SLAB = of(Material.WOOD_STEP, 1);
    static final Tile BIRCH_WOOD_SLAB = of(Material.WOOD_STEP, 2);
    static final Tile STONE_SLAB = of(Material.STEP);
    static final Tile SANDSTONE_SLAB = of(Material.STEP, 1);
    static final Tile BIRCH_FENCE = of(Material.BIRCH_FENCE);
    static final Tile CHISELED_SANDSTONE = of(Material.SANDSTONE, 1);
    static final Tile SANDSTONE = of(Material.SANDSTONE);
    static final Tile SMOOTH_SANDSTONE = of(Material.SANDSTONE, 2);

    void setBlock(Block block) {
        block.setTypeIdAndData(mat.getId(), (byte)data, true);
    }

    void setBlockNoPhysics(Block block) {
        block.setTypeIdAndData(mat.getId(), (byte)data, false);
    }

    Tile or(int data) {
        return of(mat, this.data | data);
    }
}
