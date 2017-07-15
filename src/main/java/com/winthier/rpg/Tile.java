package com.winthier.rpg;

import com.winthier.exploits.bukkit.BukkitExploits;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;

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

    boolean isSideways() {
        switch (mat) {
        case LOG:
        case LOG_2:
        case PURPUR_PILLAR:
            return (data & 12) == 4 || (data & 12) == 8;
        case QUARTZ_BLOCK:
            return data == 3 || data == 4;
        default:
            return false;
        }
    }

    Tile facingEastWest() {
        switch (mat) {
        case LOG:
        case LOG_2:
        case PURPUR_PILLAR:
            return of(mat, (data & ~8) | 4);
        case QUARTZ_BLOCK:
            return of(mat, 3);
        default:
            return this;
        }
    }

    Tile facingNorthSouth() {
        switch (mat) {
        case LOG:
        case LOG_2:
        case PURPUR_PILLAR:
            return of(mat, (data & ~4) | 8);
        case QUARTZ_BLOCK:
            return of(mat, 4);
        default:
            return this;
        }
    }

    static final Tile AIR = of(Material.AIR);

    static final Tile STONE = of(Material.STONE);
    static final Tile GRANITE = of(Material.STONE, 1);
    static final Tile POLISHED_GRANITE = of(Material.STONE, 2);
    static final Tile DIORITE = of(Material.STONE, 3);
    static final Tile POLISHED_DIORITE = of(Material.STONE, 4);
    static final Tile ANDESITE = of(Material.STONE, 5);
    static final Tile POLISHED_ANDESITE = of(Material.STONE, 6);

    static final Tile CONCRETE = of(Material.CONCRETE);
    static final Tile CONCRETE_POWDER = of(Material.CONCRETE_POWDER);
    static final Tile TERRACOTTA = of(Material.STAINED_CLAY);

    static final Tile PURPUR_BLOCK = of(Material.PURPUR_BLOCK);
    static final Tile PURPUR_PILLAR = of(Material.PURPUR_PILLAR);
    static final Tile PURPUR_SLAB = of(Material.PURPUR_SLAB);
    static final Tile PURPUR_DOUBLE_SLAB = of(Material.PURPUR_DOUBLE_SLAB);

    static final Tile END_STONE_BRICKS = of(Material.END_BRICKS);

    static final Tile OAK_FENCE = of(Material.FENCE);
    static final Tile BIRCH_FENCE = of(Material.BIRCH_FENCE);
    static final Tile SPRUCE_FENCE = of(Material.SPRUCE_FENCE);
    static final Tile JUNGLE_FENCE = of(Material.JUNGLE_FENCE);
    static final Tile ACACIA_FENCE = of(Material.ACACIA_FENCE);
    static final Tile DARK_OAK_FENCE = of(Material.DARK_OAK_FENCE);
    static final Tile NETHER_BRICK_FENCE = of(Material.NETHER_FENCE);

    static final Tile COBBLESTONE = of(Material.COBBLESTONE);
    static final Tile MOSSY_COBBLESTONE = of(Material.MOSSY_COBBLESTONE);
    static final Tile COBBLESTONE_WALL = of(Material.COBBLE_WALL);
    static final Tile GLASS_PANE = of(Material.THIN_GLASS);
    static final Tile BRICKS = of(Material.BRICK);

    static final Tile OAK_LOG = of(Material.LOG);
    static final Tile SPRUCE_LOG = of(Material.LOG, 1);
    static final Tile BIRCH_LOG = of(Material.LOG, 2);
    static final Tile JUNGLE_LOG = of(Material.LOG, 3);
    static final Tile ACACIA_LOG = of(Material.LOG_2);
    static final Tile DARK_OAK_LOG = of(Material.LOG_2, 1);

    static final Tile STONE_BRICKS = of(Material.SMOOTH_BRICK);
    static final Tile MOSSY_STONE_BRICKS = of(Material.SMOOTH_BRICK, 1);
    static final Tile CRACKED_STONE_BRICKS = of(Material.SMOOTH_BRICK, 2);
    static final Tile CHISELED_STONE_BRICKS = of(Material.SMOOTH_BRICK, 3);

    static final Tile OAK_PLANKS = of(Material.WOOD);
    static final Tile SPRUCE_PLANKS = of(Material.WOOD, 1);
    static final Tile BIRCH_PLANKS = of(Material.WOOD, 2);
    static final Tile JUNGLE_PLANKS = of(Material.WOOD, 3);
    static final Tile ACACIA_PLANKS = of(Material.WOOD, 4);
    static final Tile DARK_OAK_PLANKS = of(Material.WOOD, 5);

    static final Tile SANDSTONE = of(Material.SANDSTONE);
    static final Tile CHISELED_SANDSTONE = of(Material.SANDSTONE, 1);
    static final Tile SMOOTH_SANDSTONE = of(Material.SANDSTONE, 2);

    static final Tile RED_SANDSTONE = of(Material.RED_SANDSTONE);
    static final Tile CHISELED_RED_SANDSTONE = of(Material.RED_SANDSTONE, 1);
    static final Tile SMOOTH_RED_SANDSTONE = of(Material.RED_SANDSTONE, 2);
    static final Tile RED_SANDSTONE_STAIRS = of(Material.RED_SANDSTONE_STAIRS);
    static final Tile RED_SANDSTONE_SLAB = of(Material.STONE_SLAB2);
    static final Tile DOUBLE_RED_SANDSTONE_SLAB = of(Material.DOUBLE_STONE_SLAB2);
    static final Tile SMOOTH_DOUBLE_RED_SANDSTONE_SLAB = of(Material.DOUBLE_STONE_SLAB2, 8);

    static final Tile NETHER_BRICK = of(Material.NETHER_BRICK);
    static final Tile RED_NETHER_BRICK = of(Material.RED_NETHER_BRICK);

    static final Tile STONE_SLAB = of(Material.STEP);
    static final Tile SANDSTONE_SLAB = of(Material.STEP, 1);
    static final Tile COBBLESTONE_SLAB = of(Material.STEP, 3);
    static final Tile BRICK_SLAB = of(Material.STEP, 4);
    static final Tile STONE_BRICK_SLAB = of(Material.STEP, 5);
    static final Tile NETHER_BRICK_SLAB = of(Material.STEP, 6);
    static final Tile QUARTZ_SLAB = of(Material.STEP, 7);

    static final Tile DOUBLE_STONE_SLAB = of(Material.DOUBLE_STEP);
    static final Tile DOUBLE_SANDSTONE_SLAB = of(Material.DOUBLE_STEP, 1);
    static final Tile DOUBLE_COBBLESTONE_SLAB = of(Material.DOUBLE_STEP, 3);
    static final Tile DOUBLE_BRICK_SLAB = of(Material.DOUBLE_STEP, 4);
    static final Tile DOUBLE_STONE_BRICK_SLAB = of(Material.DOUBLE_STEP, 5);
    static final Tile DOUBLE_NETHER_BRICK_SLAB = of(Material.DOUBLE_STEP, 6);
    static final Tile DOUBLE_QUARTZ_SLAB = of(Material.DOUBLE_STEP, 7);
    static final Tile SMOOTH_DOUBLE_STONE_SLAB = of(Material.DOUBLE_STEP, 8);
    static final Tile SMOOTH_DOUBLE_SANDSTONE_SLAB = of(Material.DOUBLE_STEP, 9);

    static final Tile OAK_WOOD_SLAB = of(Material.WOOD_STEP);
    static final Tile SPRUCE_WOOD_SLAB = of(Material.WOOD_STEP, 1);
    static final Tile BIRCH_WOOD_SLAB = of(Material.WOOD_STEP, 2);
    static final Tile JUNGLE_WOOD_SLAB = of(Material.WOOD_STEP, 3);
    static final Tile ACACIA_WOOD_SLAB = of(Material.WOOD_STEP, 4);
    static final Tile DARK_OAK_WOOD_SLAB = of(Material.WOOD_STEP, 5);

    static final Tile DOUBLE_OAK_WOOD_SLAB = of(Material.WOOD_DOUBLE_STEP);
    static final Tile DOUBLE_SPRUCE_WOOD_SLAB = of(Material.WOOD_DOUBLE_STEP, 1);
    static final Tile DOUBLE_BIRCH_WOOD_SLAB = of(Material.WOOD_DOUBLE_STEP, 2);
    static final Tile DOUBLE_JUNGLE_WOOD_SLAB = of(Material.WOOD_DOUBLE_STEP, 3);
    static final Tile DOUBLE_ACACIA_WOOD_SLAB = of(Material.WOOD_DOUBLE_STEP, 4);
    static final Tile DOUBLE_DARK_OAK_WOOD_SLAB = of(Material.WOOD_DOUBLE_STEP, 5);

    static final Tile OAK_WOOD_STAIRS = of(Material.WOOD_STAIRS);
    static final Tile SPRUCE_WOOD_STAIRS = of(Material.SPRUCE_WOOD_STAIRS);
    static final Tile BIRCH_WOOD_STAIRS = of(Material.BIRCH_WOOD_STAIRS);
    static final Tile JUNGLE_WOOD_STAIRS = of(Material.JUNGLE_WOOD_STAIRS);
    static final Tile ACACIA_WOOD_STAIRS = of(Material.ACACIA_STAIRS);
    static final Tile DARK_OAK_WOOD_STAIRS = of(Material.DARK_OAK_STAIRS);

    static final Tile BRICK_STAIRS = of(Material.BRICK_STAIRS);
    static final Tile COBBLESTONE_STAIRS = of(Material.COBBLESTONE_STAIRS);
    static final Tile NETHER_BRICK_STAIRS = of(Material.NETHER_BRICK_STAIRS);
    static final Tile PURPUR_STAIRS = of(Material.PURPUR_STAIRS);
    static final Tile QUARTZ_STAIRS = of(Material.QUARTZ_STAIRS);
    static final Tile SANDSTONE_STAIRS = of(Material.SANDSTONE_STAIRS);
    static final Tile STONE_BRICK_STAIRS = of(Material.SMOOTH_STAIRS);

    static final Tile QUARTZ_BLOCK = of(Material.QUARTZ_BLOCK);
    static final Tile CHISELED_QUARTZ_BLOCK = of(Material.QUARTZ_BLOCK, 1);
    static final Tile PILLAR_QUARTZ_BLOCK = of(Material.QUARTZ_BLOCK, 2);

    void setBlock(Block block) {
        block.setTypeIdAndData(mat.getId(), (byte)data, true);
        BukkitExploits.getInstance().setPlayerPlaced(block, mat != Material.AIR);
    }

    void setBlockNoPhysics(Block block) {
        block.setTypeIdAndData(mat.getId(), (byte)data, false);
        BukkitExploits.getInstance().setPlayerPlaced(block, mat != Material.AIR);
    }

    Tile or(int data) {
        return of(mat, this.data | data);
    }
}
