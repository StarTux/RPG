package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.material.MaterialData;

final class Generator {
    final Random random = new Random(System.currentTimeMillis());
    final Set<Material> replaceMats = EnumSet.of(Material.LOG, Material.LOG_2, Material.LEAVES, Material.LEAVES_2);
    final Map<Vec2, Block> highestBlocks = new HashMap<>();
    private int npcId = 0;

    String generateTownName() {
        int syllables = 1 + random.nextInt(3);
        final String[] beginSyllable = {"b", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "qu", "r", "s", "t", "v", "w", "x", "z", "sh", "st", "sn", "sk", "sl", "sm", "ch", "kr", "fr", "gr", "tr", "y", "bl", "ph", "pl", "pr", "str", "chr", "schw"};
        final String[] vocals = {"a", "e", "i", "o", "u"};
        final String[] longVocals = {"aa", "ee", "oo"};
        final String[] diphtongs = {"au", "ei", "ou"};
        final String[] accents = {"á", "â", "à", "é", "ê", "è", "ó", "ô", "ò", "ú", "û", "ù"};
        final String[] umlauts = {"ä", "ö", "ü"};
        final String[] endSyllable = {"b", "d", "f", "g", "k", "l", "m", "n", "p", "r", "s", "t", "v", "w", "x", "z", "st", "nd", "sd", "sh", "tsh", "sch", "ng", "nk", "rk", "lt", "ld", "rn", "rt", "rs", "ts", "mb", "rst", "tch", "ch"};
        final String[] endStrong = {"ff", "gg", "kk", "ll", "mm", "nn", "pp", "rr", "ss", "tt", "tz", "ck"};
        StringBuilder sb = new StringBuilder();
        boolean priorHasEnd = true;
        boolean priorStrongEnd = false;
        int useSpecialChars = random.nextInt(2);
        for (int i = 0; i < syllables; i += 1) {
            boolean hasBegin = !priorHasEnd || random.nextBoolean();
            boolean hasEnd = random.nextBoolean();
            if (!hasBegin && !hasEnd) {
                if (random.nextBoolean()) {
                    hasBegin = true;
                } else {
                    hasEnd = true;
                }
            }
            if (hasBegin) sb.append(beginSyllable[random.nextInt(beginSyllable.length)]);
            boolean allowStrongEnd = true;
            switch (random.nextInt(8)) {
            case 0:
                switch (useSpecialChars) {
                case 0: sb.append(accents[random.nextInt(accents.length)]); break;
                case 1: default: sb.append(umlauts[random.nextInt(umlauts.length)]);
                }
                break;
            case 1: sb.append(longVocals[random.nextInt(longVocals.length)]);
                allowStrongEnd = false;
                break;
            case 2: sb.append(diphtongs[random.nextInt(diphtongs.length)]); break;
            default: sb.append(vocals[random.nextInt(vocals.length)]);
            }
            if (hasEnd) {
                if (!priorStrongEnd && allowStrongEnd && random.nextInt(3) == 0) {
                    sb.append(endStrong[random.nextInt(endStrong.length)]);
                    priorStrongEnd = true;
                } else {
                    sb.append(endSyllable[random.nextInt(endSyllable.length)]);
                    priorStrongEnd = false;
                }
            }
            priorHasEnd = hasEnd;
        }
        String result = sb.toString();
        String cleaned = cleanSpecialChars(result);
        final String[] forbiddenWords = {"nigger", "nigga", "nygger", "nygga", "penis", "penys", "dick", "fuck"};
        for (String forbiddenWord: forbiddenWords) {
            if (cleaned.contains(forbiddenWord)) {
                return generateTownName();
            }
        }
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    static String cleanSpecialChars(String string) {
        return string.replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("á", "a").replace("â", "a").replace("à", "a").replace("é", "e").replace("ê", "e").replace("è", "e").replace("ó", "o").replace("ô", "o").replace("ò", "o").replace("ú", "u").replace("û", "u").replace("ù", "u");
    }

    Town tryToPlantTown(World world, int sizeInChunks) {
        double size = world.getWorldBorder().getSize() * 0.5;
        Block blockMin = world.getWorldBorder().getCenter().add(-size, 0, -size).getBlock();
        Block blockMax = world.getWorldBorder().getCenter().add(size, 0, size).getBlock();
        int x = blockMin.getX() + random.nextInt(blockMax.getX() - blockMin.getX());
        int z = blockMin.getZ() + random.nextInt(blockMax.getZ() - blockMin.getZ());
        Chunk centerChunk = world.getBlockAt(x, 0, z).getChunk();
        return tryToPlantTown(centerChunk, sizeInChunks);
    }

    Town tryToPlantTown(Chunk centerChunk, int sizeInChunks) {
        World world = centerChunk.getWorld();
        List<Chunk> todo = new ArrayList<>();
        todo.add(centerChunk);
        Set<Chunk> done = new HashSet<>();
        List<Vec2> chunks = new ArrayList<>();
        List<Integer> allHeights = new ArrayList<>();
        while (!todo.isEmpty() && chunks.size() < sizeInChunks) {
            Chunk chunk = todo.remove(0);
            if (done.contains(chunk)) continue;
            done.add(chunk);
            List<Integer> heights = new ArrayList<>();
            for (int z = 0; z < 16; z += 1) {
                for (int x = 0; x < 16; x += 1) {
                    Block block = findHighestBlock(chunk.getBlock(x, 0, z));
                    if (block.isLiquid()) {
                        heights.add(0);
                    } else {
                        heights.add(block.getY() + 1);
                    }
                }
            }
            List<Integer> newAllHeights = new ArrayList<>(allHeights);
            newAllHeights.addAll(heights);
            Collections.sort(newAllHeights);
            int median = newAllHeights.get(newAllHeights.size() / 2);
            boolean fits = true;
            for (int i: heights) {
                if (Math.abs(i - median) > 8) {
                    fits = false;
                    break;
                }
            }
            if (median > 0 && fits) {
                allHeights.addAll(heights);
                chunks.add(new Vec2(chunk.getX(), chunk.getZ()));
                Chunk a = world.getChunkAt(chunk.getX() + 1, chunk.getZ() + 0);
                Chunk b = world.getChunkAt(chunk.getX() + 0, chunk.getZ() + 1);
                Chunk c = world.getChunkAt(chunk.getX() - 1, chunk.getZ() + 0);
                Chunk d = world.getChunkAt(chunk.getX() + 0, chunk.getZ() - 1);
                if (!done.contains(a)) todo.add(a);
                if (!done.contains(b)) todo.add(b);
                if (!done.contains(c)) todo.add(c);
                if (!done.contains(d)) todo.add(d);
            }
        }
        if (chunks.size() < sizeInChunks) return null;
        int ax = chunks.get(0).x;
        int bx = chunks.get(0).x;
        int ay = chunks.get(0).y;
        int by = chunks.get(0).y;
        for (Vec2 chunk: chunks) {
            if (chunk.x < ax) ax = chunk.x;
            if (chunk.x > bx) bx = chunk.x;
            if (chunk.y < ay) ay = chunk.y;
            if (chunk.y > by) by = chunk.y;
        }
        return new Town(ax * 16, ay * 16, bx * 16 + 15, by * 16 + 15, chunks);
    }

    void plantTown(World world, Town town, Set<Flag> flags) {
        Collections.shuffle(town.chunks);
        for (Vec2 chunk: town.chunks) {
            int width = 6 + random.nextInt(9);
            int height = 6 + random.nextInt(9);
            int offx = width < 14 ? 1 + random.nextInt(14 - width) : 1;
            int offy = height < 14 ? 1 + random.nextInt(14 - height) : 1;
            House house = generateHouse(width, height, flags);
            town.houses.add(house);
            house.townId = town.townId;
            plantHouse(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), house, flags);
        }
    }

    void plantMonument(Block center) {
        Block block = findHighestBlock(center);
    }

    void plantHouse(Block start, House house, Set<Flag> flags) {
        Map<Vec2, RoomTile> tiles = house.tiles;
        Flag flagStyle = Flag.RANDOM;
        Flag flagDoor = Flag.RANDOM;
        Flag flagAltitude = Flag.SURFACE;
        Flag flagNPC = Flag.RANDOM;
        for (Flag flag: flags) {
            if (flag.strategy == Flag.Strategy.STYLE) flagStyle = flag;
            if (flag.strategy == Flag.Strategy.DOOR) flagDoor = flag;
            if (flag.strategy == Flag.Strategy.ALTITUDE) flagAltitude = flag;
            if (flag.strategy == Flag.Strategy.NPC) flagNPC = flag;
        }
        boolean noRoof = flags.contains(Flag.NO_ROOF) || flagAltitude == Flag.UNDERGROUND;
        boolean noBase = flags.contains(Flag.NO_BASE) || flagAltitude != Flag.SURFACE;
        boolean noDecoration = flags.contains(Flag.NO_DECORATION);
        boolean noNPCs = flags.contains(Flag.NO_NPCS);
        int floorLevel;
        switch (flagAltitude) {
        case UNDERGROUND:
            floorLevel = 4 + random.nextInt(40);
            break;
        case FLOATING:
            floorLevel = 128 + random.nextInt(100);
            break;
        case HERE:
            floorLevel = start.getY() - 1;
            break;
        case SURFACE:
        default:
            List<Integer> floorLevels = new ArrayList<>(tiles.size());
            for (Vec2 vec: tiles.keySet()) {
                Block highest = findHighestBlock(start.getWorld().getBlockAt(start.getX() + vec.x, 0, start.getZ() + vec.y));
                floorLevels.add(highest.getY());
            }
            Collections.sort(floorLevels);
            floorLevel = floorLevels.get(floorLevels.size() / 2);
        }
        {
            int ax, az, bx, bz;
            ax = az = Integer.MAX_VALUE;
            bx = bz = Integer.MIN_VALUE;
            int ay = floorLevel;
            int by = floorLevel + 4;
            house.boundingBox = new Cuboid(ax, ay, az, bx, by, bz);
            for (Room room: house.rooms) {
                Cuboid bb = new Cuboid(room.ax + start.getX(), ay, room.ay + start.getZ(),
                                       room.bx + start.getX(), by, room.by + start.getZ());
                room.boundingBox = bb;
                if (ax > bb.ax) ax = bb.ax;
                if (az > bb.az) az = bb.az;
                if (bx < bb.bx) bx = bb.bx;
                if (bz < bb.bz) bz = bb.bz;
            }
            house.boundingBox = new Cuboid(ax, ay, az, bx, by, bz);
        }
        Material matDoor;
        switch (flagDoor) {
        case RANDOM:
            switch (flagStyle) {
            case WOOD: matDoor = Material.WOODEN_DOOR; break;
            case SPRUCE_WOOD: matDoor = Material.SPRUCE_DOOR; break;
            case BIRCH_WOOD: matDoor = Material.BIRCH_DOOR; break;
            case JUNGLE_WOOD: matDoor = Material.JUNGLE_DOOR; break;
            case ACACIA_WOOD: matDoor = Material.ACACIA_DOOR; break;
            case DARK_OAK_WOOD: matDoor = Material.DARK_OAK_DOOR; break;
            default:
                switch (random.nextInt(7)) {
                case 0: matDoor = Material.ACACIA_DOOR; break;
                case 1: matDoor = Material.BIRCH_DOOR; break;
                case 2: matDoor = Material.DARK_OAK_DOOR; break;
                case 3: matDoor = Material.IRON_DOOR_BLOCK; break;
                case 4: matDoor = Material.JUNGLE_DOOR; break;
                case 5: matDoor = Material.SPRUCE_DOOR; break;
                case 6: default: matDoor = Material.WOODEN_DOOR;
                }
            }
            break;
        case ACACIA_DOOR: matDoor = Material.ACACIA_DOOR; break;
        case BIRCH_DOOR: matDoor = Material.BIRCH_DOOR; break;
        case DARK_OAK_DOOR: matDoor = Material.DARK_OAK_DOOR; break;
        case IRON_DOOR: matDoor = Material.IRON_DOOR_BLOCK; break;
        case JUNGLE_DOOR: matDoor = Material.JUNGLE_DOOR; break;
        case SPRUCE_DOOR: matDoor = Material.SPRUCE_DOOR; break;
        case OAK_DOOR: default: matDoor = Material.WOODEN_DOOR;
        }
        int color = random.nextInt(16);
        for (Vec2 vec: tiles.keySet()) {
            Block floor = start.getWorld().getBlockAt(start.getX() + vec.x, floorLevel, start.getZ() + vec.y);
            Block[] blocks = {
                floor, floor.getRelative(0, 1, 0), floor.getRelative(0, 2, 0), floor.getRelative(0, 3, 0), floor.getRelative(0, 4, 0)
            };
            RoomTile tile = tiles.get(vec);
            RoomTile tileEast = tiles.get(vec.relative(1, 0));
            RoomTile tileSouth = tiles.get(vec.relative(0, 1));
            RoomTile tileWest = tiles.get(vec.relative(-1, 0));
            RoomTile tileNorth = tiles.get(vec.relative(0, -1));
            boolean tileIsWall = tile != null && tile != RoomTile.FLOOR;
            boolean eastIsWall = tileEast != null && tileEast != RoomTile.FLOOR;
            boolean southIsWall = tileSouth != null && tileSouth != RoomTile.FLOOR;
            boolean westIsWall = tileWest != null && tileWest != RoomTile.FLOOR;
            boolean northIsWall = tileNorth != null && tileNorth != RoomTile.FLOOR;
            boolean tileIsCorner = tileIsWall && ((eastIsWall && southIsWall) || (southIsWall && westIsWall) || (westIsWall && northIsWall) || (northIsWall && eastIsWall));
            // Materials
            int dataFloor, dataCeil, dataBase;
            dataFloor = dataCeil = dataBase = 0;
            Material matBase = Material.COBBLESTONE;
            Material matFloor = Material.WOOD;
            Material matCeil = Material.WOOD;
            Material[] mats = { Material.COBBLESTONE, Material.COBBLESTONE, Material.COBBLESTONE, Material.COBBLESTONE, Material.COBBLESTONE };
            int[] data = { 0, 0, 0, 0, 0 };
            switch (flagStyle) {
            case COBBLE:
                mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.COBBLESTONE;
                mats[random.nextInt(mats.length)] = Material.MOSSY_COBBLESTONE;
                matBase = Material.COBBLESTONE;
                matCeil = matFloor = Material.WOOD;
                break;
            case SANDSTONE:
                matFloor = Material.DOUBLE_STEP;
                mats[0] = mats[1] = mats[4] = matCeil = matBase = Material.SANDSTONE;
                if (tileIsCorner) {
                    mats[2] = mats[3] = Material.SANDSTONE;
                    data[0] = data[2] = data[3] = dataBase = 2;
                    data[1] = data[4] = 1;
                } else {
                    mats[2] = mats[3] = Material.DOUBLE_STEP;
                    data[2] = data[3] = 9;
                    data[4] = 2;
                }
                break;
            case RED_SANDSTONE:
                matFloor = Material.DOUBLE_STEP;
                mats[0] = mats[1] = mats[4] = matCeil = matBase = Material.RED_SANDSTONE;
                if (tileIsCorner) {
                    mats[2] = mats[3] = Material.RED_SANDSTONE;
                    data[0] = data[2] = data[3] = dataBase = 2;
                    data[1] = data[4] = 1;
                } else {
                    mats[2] = mats[3] = Material.DOUBLE_STONE_SLAB2;
                    data[2] = data[3] = 8;
                    data[4] = 2;
                }
                break;
            case QUARTZ:
                matCeil = matFloor = Material.DOUBLE_STEP;
                mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.QUARTZ_BLOCK;
                if (tileIsCorner) {
                    matBase = Material.QUARTZ_BLOCK;
                    dataBase = data[0] = data[1] = data[2] = data[3] = data[4] = dataBase = 2;
                } else {
                    data[1] = 1;
                    data[4] = 1;
                    matBase = Material.DOUBLE_STEP;
                    dataBase = 8;
                }
                break;
            case STONEBRICK:
                matFloor = Material.WOOD;
                matCeil = Material.WOOD;
                mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.SMOOTH_BRICK;
                data[random.nextInt(data.length)] = random.nextInt(3);
                matBase = Material.SMOOTH_BRICK;
                break;
            case BRICKS:
                matFloor = matCeil = Material.WOOD;
                dataFloor = dataCeil = 1;
                mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = matBase = Material.BRICK;
                break;
            case KOONTZY:
                matFloor = matCeil = Material.WOOD;
                dataFloor = dataCeil = 1;
                if (tileIsCorner) {
                    matBase = Material.LOG;
                    dataBase = 1;
                    for (int i = 0; i < mats.length; i += 1) {
                        mats[i] = Material.LOG;
                        data[i] = 1;
                    }
                } else {
                    matBase = Material.BRICK;
                    for (int i = 0; i < mats.length; i += 1) {
                        if (random.nextBoolean()) {
                            mats[i] = Material.STAINED_CLAY;
                            data[i] = color;
                        } else {
                            mats[i] = Material.BRICK;
                        }
                    }
                }
                break;
            case WOOD:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.LOG;
                } else {
                    data[4] = (eastIsWall || westIsWall) ? 4 : 8;
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.WOOD;
                }
                mats[4] = Material.LOG;
                matFloor = matCeil = Material.COBBLESTONE;
                break;
            case SPRUCE_WOOD:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.LOG;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 1;
                    data[4] = 1;
                } else {
                    data[4] = (eastIsWall || westIsWall) ? 5 : 9;
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.WOOD;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 1;
                }
                mats[4] = Material.LOG;
                matFloor = matCeil = Material.COBBLESTONE;
                break;
            case BIRCH_WOOD:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.LOG;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 2;
                    data[4] = 2;
                } else {
                    data[4] = (eastIsWall || westIsWall) ? 6 : 10;
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.WOOD;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 2;
                }
                mats[4] = Material.LOG;
                matFloor = matCeil = Material.COBBLESTONE;
                break;
            case JUNGLE_WOOD:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.LOG;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 3;
                    data[4] = 3;
                } else {
                    data[4] = (eastIsWall || westIsWall) ? 7 : 11;
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.WOOD;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 3;
                }
                mats[4] = Material.LOG;
                matFloor = matCeil = Material.COBBLESTONE;
                break;
            case ACACIA_WOOD:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.LOG_2;
                } else {
                    data[4] = (eastIsWall || westIsWall) ? 4 : 8;
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.WOOD;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 4;
                }
                mats[4] = Material.LOG_2;
                matFloor = matCeil = Material.COBBLESTONE;
                break;
            case DARK_OAK_WOOD:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.LOG_2;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 1;
                    data[4] = 1;
                } else {
                    data[4] = (eastIsWall || westIsWall) ? 5 : 9;
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.WOOD;
                    dataBase = data[0] = data[1] = data[2] = data[3] = 5;
                }
                mats[4] = Material.LOG_2;
                matFloor = matCeil = Material.COBBLESTONE;
                break;
            case CONCRETE:
                mats[0] = mats[4] = Material.STONE;
                data[0] = data[4] = 6;
                if (tileIsCorner) {
                    matBase = mats[1] = mats[2] = mats[3] = Material.STONE;
                    dataBase = data[1] = data[2] = data[3] = 6;
                } else {
                    matBase = mats[1] = mats[2] = mats[3] = Material.CONCRETE;
                    dataBase = data[1] = data[2] = data[3] = color;
                }
                matFloor = matCeil = Material.WOOD;
                dataFloor = dataCeil = 1;
                break;
            case TERRACOTTA:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.SMOOTH_BRICK;
                } else {
                    mats[4] = Material.LOG;
                    data[4] = (eastIsWall || westIsWall) ? 5 : 9;
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.STAINED_CLAY;
                    dataBase = data[0] = data[1] = data[2] = data[3] = color;
                }
                matFloor = matCeil = Material.WOOD;
                dataFloor = dataCeil = 1;
                break;
            case STONE:
                if (tileIsCorner) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.SMOOTH_BRICK;
                } else {
                    matBase = Material.COBBLESTONE;
                    mats[0] = mats[2] = mats[3] = Material.STONE;
                    mats[1] = Material.COBBLESTONE;
                    mats[4] = Material.SMOOTH_BRICK;
                }
                matFloor = matCeil = Material.WOOD;
                break;
            case GRANITE:
                matBase = mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.STONE;
                matFloor = matCeil = Material.WOOD;
                if (tileIsCorner) {
                    dataBase = data[0] = data[1] = data[2] = data[3] = data[4] = 2;
                } else {
                    data[1] = data[4] = 2;
                    dataBase = data[0] = data[2] = data[3] = 1;
                }
                break;
            case DIORITE:
                matBase = mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.STONE;
                matFloor = matCeil = Material.WOOD;
                if (tileIsCorner) {
                    dataBase = data[0] = data[1] = data[2] = data[3] = data[4] = 4;
                } else {
                    data[1] = data[4] = 4;
                    dataBase = data[0] = data[2] = data[3] = 3;
                }
                break;
            case ANDESITE:
                matBase = mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.STONE;
                matFloor = matCeil = Material.WOOD;
                if (tileIsCorner) {
                    dataBase = data[0] = data[1] = data[2] = data[3] = data[4] = 6;
                } else {
                    data[1] = data[4] = 6;
                    dataBase = data[0] = data[2] = data[3] = 5;
                }
                break;
            case PURPUR:
                if (tileIsCorner) {
                    matBase = mats[1] = mats[2] = mats[3] = Material.PURPUR_PILLAR;
                    mats[0] = mats[4] = Material.PURPUR_BLOCK;
                } else {
                    matBase = mats[0] = Material.PURPUR_BLOCK;
                    mats[1] = mats[2] = mats[3] = Material.END_BRICKS;
                    mats[4] = Material.PURPUR_PILLAR;
                    data[4] = (eastIsWall || westIsWall) ? 4 : 8;
                }
                matFloor = (vec.x & 1) == (vec.y & 1) ? Material.PURPUR_BLOCK : Material.PURPUR_PILLAR;
                matCeil = Material.PURPUR_PILLAR;
                break;
            case NETHER_BRICK:
                matCeil = matFloor = matBase = mats[0] = mats[2] = mats[3] = mats[4] = Material.NETHER_BRICK;
                mats[1] = Material.RED_NETHER_BRICK;
                break;
            default: break;
            }
            // Make a base
            if (!noBase) {
                Block block = floor.getRelative(0, -1, 0);
                while (true) {
                    Material mat = block.getType();
                    boolean doIt = !mat.isSolid() || replaceMats.contains(mat);
                    if (!doIt) break;
                    block.setTypeIdAndData(matBase.getId(), (byte)dataBase, true);
                    block = block.getRelative(0, -1, 0);
                }
            }
            switch (tile) {
            case WALL:
                for (int i = 0; i < blocks.length; i += 1) {
                    blocks[i].setTypeIdAndData(mats[i].getId(), (byte)data[i], true);
                }
                break;
            case FLOOR:
                for (int i = 0; i < blocks.length; i += 1) {
                    switch (i) {
                    case 0:
                        blocks[i].setTypeIdAndData(matFloor.getId(), (byte)dataFloor, true);
                        break;
                    case 1:
                    case 2:
                    case 3:
                        blocks[i].setType(Material.AIR);
                        break;
                    case 4:
                        blocks[i].setTypeIdAndData(matCeil.getId(), (byte)dataCeil, true);
                        break;
                    default:
                        blocks[i].setTypeIdAndData(mats[i].getId(), (byte)data[i], true);
                    }
                }
                break;
            case DOOR:
                RoomTile nbor1 = tiles.get(vec.relative(-1, 0));
                RoomTile nbor2 = tiles.get(vec.relative(1, 0));
                int dataDoor;
                if (nbor1 == null || nbor1 == RoomTile.FLOOR || nbor2 == null || nbor2 == RoomTile.FLOOR) {
                    dataDoor = random.nextBoolean() ? 0 : 2;
                } else {
                    dataDoor = random.nextBoolean() ? 1 : 3;
                }
                for (int i = 0; i < blocks.length; i += 1) {
                    switch (i) {
                    case 1:
                        blocks[i].setTypeIdAndData(matDoor.getId(), (byte)dataDoor, false);
                        break;
                    case 2:
                        blocks[i].setTypeIdAndData(matDoor.getId(), (byte)0x8, false);
                        break;
                    default:
                        blocks[i].setTypeIdAndData(mats[i].getId(), (byte)data[i], true);
                    }
                }
                break;
            case WINDOW:
                for (int i = 0; i < blocks.length; i += 1) {
                    switch (i) {
                    case 2:
                        blocks[i].setType(Material.THIN_GLASS);
                        break;
                    default:
                        blocks[i].setTypeIdAndData(mats[i].getId(), (byte)data[i], true);
                    }
                }
                break;
            }
        }
        // Decorations
        if (!noDecoration) {
            for (Room room: house.rooms) {
                int amount = room.width() + room.height();
                int torches = 1 + Math.max(room.width(), room.height()) / 4;
                for (int i = 0; i < amount; i += 1) {
                    int x, y;
                    Facing facing;
                    if (random.nextBoolean()) {
                        x = room.ax + 1 + random.nextInt(room.width() - 2);
                        if (random.nextBoolean()) {
                            y = room.ay + 1;
                            facing = Facing.SOUTH;
                        } else {
                            y = room.by - 1;
                            facing = Facing.NORTH;
                        }
                    } else {
                        y = room.ay + 1 + random.nextInt(room.height() - 2);
                        if (random.nextBoolean()) {
                            x = room.ax + 1;
                            facing = Facing.EAST;
                        } else {
                            x = room.bx - 1;
                            facing = Facing.WEST;
                        }
                    }
                    Vec2 vec = new Vec2(x, y);
                    if (tiles.get(vec) == RoomTile.FLOOR) {
                        RoomTile[] nbors = {
                            tiles.get(vec.relative(0, -1)),
                            tiles.get(vec.relative(1, 0)),
                            tiles.get(vec.relative(0, 1)),
                            tiles.get(vec.relative(-1, 0)) };
                        boolean skip = false;
                        for (int j = 0; j < 4 && !skip; j += 1) {
                            RoomTile nbor = nbors[j];
                            if (nbor == RoomTile.WINDOW || nbor == RoomTile.DOOR || nbor == RoomTile.DECORATION) skip = true;
                        }
                        if (!skip) {
                            Block floor = start.getWorld().getBlockAt(start.getX() + vec.x, floorLevel, start.getZ() + vec.y);
                            house.tiles.put(vec, RoomTile.DECORATION);
                            if (torches > 0) {
                                torches -= 1;
                                floor.getRelative(0, 2, 0).setTypeIdAndData(Material.TORCH.getId(), (byte)facing.dataTorch, true);
                            } else {
                                switch (random.nextInt(20)) {
                                case 0:
                                    floor.getRelative(0, 3, 0).setType(Material.BOOKSHELF);
                                    // fallthrough
                                case 1:
                                    floor.getRelative(0, 2, 0).setType(Material.BOOKSHELF);
                                    // fallthrough
                                case 2:
                                    floor.getRelative(0, 1, 0).setType(Material.BOOKSHELF);
                                    break;
                                case 3:
                                    floor.getRelative(0, 1, 0).setType(Material.CAULDRON);
                                    break;
                                case 4:
                                    floor.getRelative(0, 1, 0).setType(Material.BREWING_STAND);
                                    break;
                                case 5:
                                    floor.getRelative(0, 1, 0).setType(Material.JUKEBOX);
                                    break;
                                case 6:
                                    floor.getRelative(0, 1, 0).setType(Material.WORKBENCH);
                                    break;
                                case 7:
                                    floor.getRelative(0, 1, 0).setTypeIdAndData(Material.FURNACE.getId(), (byte)facing.dataBlock, true);
                                    break;
                                case 8:
                                    floor.getRelative(0, 1, 0).setTypeIdAndData(Material.CHEST.getId(), (byte)facing.dataBlock, true);
                                    break;
                                case 9:
                                    floor.getRelative(0, 2, 0).setTypeIdAndData(Material.REDSTONE_TORCH_ON.getId(), (byte)facing.dataTorch, true);
                                    break;
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                    if (random.nextBoolean()) {
                                        floor.getRelative(0, 1, 0).setTypeIdAndData(Material.COBBLESTONE_STAIRS.getId(), (byte)(facing.dataStair | 0x4), true);
                                    } else {
                                        floor.getRelative(0, 1, 0).setTypeIdAndData(Material.WOOD_STAIRS.getId(), (byte)(facing.dataStair | 0x4), true);
                                    }
                                    floor.getRelative(0, 2, 0).setType(Material.FLOWER_POT);
                                    MaterialData flower;
                                    switch (random.nextInt(32)) {
                                    case 0: flower = new MaterialData(Material.RED_ROSE, (byte)0); break;
                                    case 1: flower = new MaterialData(Material.RED_ROSE, (byte)1); break;
                                    case 2: flower = new MaterialData(Material.RED_ROSE, (byte)2); break;
                                    case 3: flower = new MaterialData(Material.RED_ROSE, (byte)3); break;
                                    case 4: flower = new MaterialData(Material.RED_ROSE, (byte)4); break;
                                    case 5: flower = new MaterialData(Material.RED_ROSE, (byte)5); break;
                                    case 6: flower = new MaterialData(Material.RED_ROSE, (byte)6); break;
                                    case 7: flower = new MaterialData(Material.RED_ROSE, (byte)7); break;
                                    case 8: flower = new MaterialData(Material.RED_ROSE, (byte)8); break;
                                    case 9: flower = new MaterialData(Material.YELLOW_FLOWER); break;
                                    case 10: flower = new MaterialData(Material.SAPLING, (byte)0); break;
                                    case 11: flower = new MaterialData(Material.SAPLING, (byte)1); break;
                                    case 12: flower = new MaterialData(Material.SAPLING, (byte)2); break;
                                    case 13: flower = new MaterialData(Material.SAPLING, (byte)3); break;
                                    case 14: flower = new MaterialData(Material.SAPLING, (byte)4); break;
                                    case 15: flower = new MaterialData(Material.SAPLING, (byte)5); break;
                                    case 16: flower = new MaterialData(Material.RED_MUSHROOM); break;
                                    case 17: flower = new MaterialData(Material.BROWN_MUSHROOM); break;
                                    case 18: flower = new MaterialData(Material.DEAD_BUSH); break;
                                    case 19: flower = new MaterialData(Material.LONG_GRASS, (byte)2); break;
                                    case 20: default: flower = new MaterialData(Material.CACTUS); break;
                                    }
                                    org.bukkit.block.FlowerPot pot = (org.bukkit.block.FlowerPot)floor.getRelative(0, 2, 0).getState();
                                    pot.setContents(flower);
                                    pot.update();
                                    break;
                                case 17:
                                    floor.getRelative(0, 1, 0).setTypeIdAndData(Material.ANVIL.getId(), (byte)random.nextInt(12), true);
                                    break;
                                case 18:
                                    floor.getRelative(0, 2, 0).setTypeIdAndData(Material.WALL_BANNER.getId(), (byte)facing.dataBlock, true);
                                    org.bukkit.block.Banner banner = (org.bukkit.block.Banner)floor.getRelative(0, 2, 0).getState();
                                    banner.setBaseColor(DyeColor.values()[random.nextInt(DyeColor.values().length)]);
                                    int patternCount = 1 + random.nextInt(4);
                                    List<Pattern> patterns = new ArrayList<>(patternCount);
                                    for (int j = 0; j < patternCount; j += 1) {
                                        patterns.add(new Pattern(DyeColor.values()[random.nextInt(DyeColor.values().length)], PatternType.values()[random.nextInt(PatternType.values().length)]));
                                    }
                                    banner.setPatterns(patterns);
                                    banner.update();
                                    break;
                                case 19:
                                default:
                                    floor.getRelative(0, 1, 0).setType(Material.FENCE);
                                    floor.getRelative(0, 2, 0).setType(Material.WOOD_PLATE);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        // Make a roof
        if (!noRoof) {
            Material matStair = Material.WOOD_STAIRS;
            Material doubleSlabMat = Material.WOOD_DOUBLE_STEP;
            int doubleSlabData = 0;
            Material slabMat = Material.WOOD_STEP;
            int slabData = 0;
            switch (flagStyle) {
            case SPRUCE_WOOD:
                doubleSlabData = slabData = 1;
                matStair = Material.SPRUCE_WOOD_STAIRS;
                break;
            case BIRCH_WOOD:
                doubleSlabData = slabData = 2;
                matStair = Material.BIRCH_WOOD_STAIRS;
                break;
            case JUNGLE_WOOD:
                doubleSlabData = slabData = 3;
                matStair = Material.JUNGLE_WOOD_STAIRS;
                break;
            case ACACIA_WOOD:
                doubleSlabData = slabData = 4;
                matStair = Material.ACACIA_STAIRS;
                break;
            case DARK_OAK_WOOD:
                doubleSlabData = slabData = 5;
                matStair = Material.DARK_OAK_STAIRS;
                break;
            case BRICKS:
            case TERRACOTTA:
            case KOONTZY:
                matStair = Material.SPRUCE_WOOD_STAIRS;
                doubleSlabData = slabData = 1;
                break;
            case SANDSTONE:
                matStair = Material.JUNGLE_WOOD_STAIRS;
                doubleSlabData = slabData = 3;
                break;
            case RED_SANDSTONE:
                matStair = Material.ACACIA_STAIRS;
                doubleSlabData = slabData = 4;
                break;
            case QUARTZ:
                matStair = Material.SMOOTH_STAIRS;
                doubleSlabMat = Material.DOUBLE_STEP;
                slabMat = Material.STEP;
                doubleSlabData = slabData = 5;
                break;
            case PURPUR:
                matStair = Material.PURPUR_STAIRS;
                doubleSlabMat = Material.PURPUR_DOUBLE_SLAB;
                slabMat = Material.PURPUR_SLAB;
                break;
            case NETHER_BRICK:
                matStair = Material.NETHER_BRICK_STAIRS;
                doubleSlabMat = Material.DOUBLE_STEP;
                slabMat = Material.STEP;
                slabData = doubleSlabData = 6;
                break;
            default: break;
            }
            Map<Vec2, Integer> roofs = new HashMap<>();
            for (Vec2 vec: tiles.keySet()) {
                roofs.put(vec, 0);
                roofs.put(vec.relative( 1,  0), 0);
                roofs.put(vec.relative(-1,  0), 0);
                roofs.put(vec.relative( 0,  1), 0);
                roofs.put(vec.relative( 0, -1), 0);
                roofs.put(vec.relative( 1,  1), 0);
                roofs.put(vec.relative( 1, -1), 0);
                roofs.put(vec.relative(-1,  1), 0);
                roofs.put(vec.relative(-1, -1), 0);
            }
            int relx, rely;
            Orientation roofOri;
            if (random.nextBoolean()) {
                relx = 1; rely = 0;
                roofOri = Orientation.HORIZONTAL;
            } else {
                relx = 0; rely = 1;
                roofOri = Orientation.VERTICAL;
            }
            int highestRoof = 0;
            for (int i = 0; ; i += 1) {
                List<Vec2> raiseRoofs = new ArrayList<>();
                Integer roofLevel = i;
                for (Vec2 vec: roofs.keySet()) {
                    if (roofs.get(vec) == roofLevel
                        && roofs.get(vec.relative(relx, rely)) == roofLevel
                        && roofs.get(vec.relative(-relx, -rely)) == roofLevel) {
                        raiseRoofs.add(vec);
                    }
                }
                if (raiseRoofs.isEmpty()) break;
                for (Vec2 vec: raiseRoofs) roofs.put(vec, i + 1);
                highestRoof = i + 1;
            }
            boolean useStairs = random.nextBoolean();
            for (Vec2 vec: roofs.keySet()) {
                int roofLevel = roofs.get(vec);
                Block roof1 = start.getWorld().getBlockAt(start.getX() + vec.x, floorLevel + 5, start.getZ() + vec.y);
                Block roof2 = roof1.getRelative(0, useStairs ? roofLevel : roofLevel / 2, 0);
                RoomTile tile = tiles.get(vec);
                if (tile != null) {
                    while (roof1.getY() < roof2.getY()) {
                        if (!roof1.getType().isSolid() || replaceMats.contains(roof1.getType())) {
                            roof1.setTypeIdAndData(doubleSlabMat.getId(), (byte)doubleSlabData, true);
                        }
                        roof1 = roof1.getRelative(0, 1, 0);
                    }
                }
                if (!roof2.getType().isSolid() || replaceMats.contains(roof2.getType())) {
                    if (useStairs) {
                        Integer nbor1 = roofs.get(vec.relative(relx, rely));
                        Integer nbor2 = roofs.get(vec.relative(-relx, -rely));
                        if (nbor1 == null) nbor1 = -1;
                        if (nbor2 == null) nbor2 = -1;
                        if (roofLevel == highestRoof && nbor1 < roofLevel && nbor2 < roofLevel) {
                            roof2.setTypeIdAndData(slabMat.getId(), (byte)slabData, true);
                        } else {
                            int dataStair;
                            switch (roofOri) {
                            case HORIZONTAL:
                                if (nbor1 < roofLevel) {
                                    dataStair = Facing.EAST.dataStair;
                                } else {
                                    dataStair = Facing.WEST.dataStair;
                                }
                                break;
                            case VERTICAL: default:
                                if (nbor1 < roofLevel) {
                                    dataStair = Facing.SOUTH.dataStair;
                                } else {
                                    dataStair = Facing.NORTH.dataStair;
                                }
                                break;
                            }
                            roof2.setTypeIdAndData(matStair.getId(), (byte)dataStair, true);
                        }
                    } else {
                        if ((roofLevel & 1) == 0) {
                            roof2.setTypeIdAndData(slabMat.getId(), (byte)slabData, true);
                        } else {
                            if (tiles.containsKey(vec)) {
                                roof2.setTypeIdAndData(doubleSlabMat.getId(), (byte)doubleSlabData, true);
                            } else {
                                roof2.setTypeIdAndData(slabMat.getId(), (byte)(slabData | 8), true);
                            }
                        }
                    }
                }
            }
        }
        // Spawn NPCs
        if (!noNPCs) {
            int totalNPCs = Math.max(1, random.nextInt(house.rooms.size()) - random.nextInt(house.rooms.size()));
            List<Vec2> possibleNPCSpots = new ArrayList<>();
            for (Vec2 vec: tiles.keySet()) {
                if (tiles.get(vec) == RoomTile.FLOOR) {
                    possibleNPCSpots.add(vec);
                }
            }
            Collections.shuffle(possibleNPCSpots, random);
            totalNPCs = Math.min(possibleNPCSpots.size(), totalNPCs);
            for (int i = 0; i < totalNPCs; i += 1) {
                Vec2 vec = possibleNPCSpots.get(i);
                Block block = start.getWorld().getBlockAt(start.getX() + vec.x, floorLevel + 1, start.getZ() + vec.y);
                Location loc = block.getLocation().add(0.5, 0, 0.5);
                Map<String, Object> config = new HashMap<>();
                config.put("town_id", house.townId);
                config.put("npc_id", npcId);
                String typeString;
                switch (flagNPC) {
                case UNDEAD:
                    switch (random.nextInt(4)) {
                    case 0: typeString = "stray"; break;
                    case 1: typeString = "husk"; break;
                    case 2: typeString = "skeleton"; break;
                    case 3: default: typeString = "zombie"; break;
                    }
                    break;
                case VILLAGER: default: typeString = "villager";
                }
                config.put("type", typeString);
                NPCEntity.Watcher watcher = (NPCEntity.Watcher)CustomPlugin.getInstance().getEntityManager().spawnEntity(loc, NPCEntity.CUSTOM_ID, config);
                watcher.setIds(house.townId, npcId);
                npcId += 1;
                house.tiles.put(vec, RoomTile.NPC);
                house.npcs.add(new Vec3(block.getX(), block.getY(), block.getZ()));
            }
        }
    }

    House generateHouse(int width, int height, Set<Flag> flags) {
        Map<Vec2, RoomTile> tiles = new HashMap<>();
        Map<Vec2, Room> roomMap = new HashMap<>();
        Map<Room, Set<Room>> roomConnections = new HashMap<>();
        List<Room> rooms = splitRoom(new Room(0, 0, width, height));
        // Remove rooms
        if (rooms.size() > 2) {
            List<Room> removableRooms = rooms.stream().filter(r -> r.ax == 0 || r.ay == 0 || r.bx == width || r.by == height).collect(Collectors.toList());
            Collections.shuffle(removableRooms, random);
            int removeCount = Math.min(rooms.size() - 2, 1 + random.nextInt(removableRooms.size()));
            for (int i = 0; i < removeCount; i += 1) {
                Room room = removableRooms.get(i);
                rooms.remove(room);
                if (!roomsConnected(rooms)) rooms.add(room);
            }
        }
        // Draw tiles
        for (Room room: rooms) {
            for (int y = room.ay; y <= room.by; y += 1) {
                tiles.put(new Vec2(room.ax, y), RoomTile.WALL);
                tiles.put(new Vec2(room.bx, y), RoomTile.WALL);
            }
            for (int x = room.ax; x <= room.bx; x += 1) {
                tiles.put(new Vec2(x, room.ay), RoomTile.WALL);
                tiles.put(new Vec2(x, room.by), RoomTile.WALL);
            }
            for (int y = room.ay + 1; y < room.by; y += 1) {
                for (int x = room.ax + 1; x < room.bx; x += 1) {
                    Vec2 vec = new Vec2(x, y);
                    tiles.put(vec, RoomTile.FLOOR);
                    roomMap.put(vec, room);
                }
            }
        }
        // Insert doors
        Collections.shuffle(rooms, random);
        int totalOutsideDoors = 1 + random.nextInt(3);
        for (Room room: rooms) {
            Set<Room> connectedRooms = roomConnections.get(room);
            if (connectedRooms == null) {
                connectedRooms = new HashSet<>();
                roomConnections.put(room, connectedRooms);
            }
            Map<Vec2, Facing> facing = new HashMap<>();
            for (int x = room.ax + 1; x < room.bx; x += 1) {
                facing.put(new Vec2(x, room.ay), Facing.NORTH);
                facing.put(new Vec2(x, room.by), Facing.SOUTH);
            }
            for (int y = room.ay + 1; y < room.by; y += 1) {
                facing.put(new Vec2(room.ax, y), Facing.WEST);
                facing.put(new Vec2(room.bx, y), Facing.EAST);
            }
            List<Vec2> walls = new ArrayList<>(facing.keySet());
            Collections.shuffle(walls, random);
            boolean outsideDoor = false;
            for (Vec2 vec: walls) {
                Vec2 vecInside, vecOutside, vecLeft, vecRight;
                switch (facing.get(vec)) {
                case NORTH:
                    vecInside = vec.relative(0,  1);
                    vecOutside = vec.relative(0, -1);
                    vecLeft = vec.relative(-1, 0);
                    vecRight = vec.relative(1, 0);
                    break;
                case SOUTH:
                    vecInside = vec.relative(0, -1);
                    vecOutside = vec.relative(0, 1);
                    vecLeft = vec.relative(1, 0);
                    vecRight = vec.relative(-1, 0);
                    break;
                case WEST:
                    vecInside = vec.relative(1, 0);
                    vecOutside = vec.relative(-1, 0);
                    vecLeft = vec.relative(0, 1);
                    vecRight = vec.relative(0, -1);
                    break;
                case EAST:
                default:
                    vecInside = vec.relative(-1, 0);
                    vecOutside = vec.relative(1, 0);
                    vecLeft = vec.relative(0, -1);
                    vecRight = vec.relative(0, 1);
                    break;
                }
                RoomTile wall = tiles.get(vec);
                RoomTile inside = tiles.get(vecInside);
                RoomTile outside = tiles.get(vecOutside);
                RoomTile left = tiles.get(vecLeft);
                RoomTile right = tiles.get(vecRight);
                if (wall == RoomTile.WALL && inside == RoomTile.FLOOR && left == RoomTile.WALL && right == RoomTile.WALL) {
                    if (outside == null) {
                        if (!flags.contains(Flag.UNDERGROUND)) {
                            if (!outsideDoor && totalOutsideDoors > 0) {
                                tiles.put(vec, RoomTile.DOOR);
                                outsideDoor = true;
                                totalOutsideDoors -= 1;
                            } else {
                                if (random.nextBoolean()) {
                                    tiles.put(vec, RoomTile.WINDOW);
                                }
                            }
                        }
                    } else if (outside == RoomTile.FLOOR) {
                        Room otherRoom = roomMap.get(vecOutside);
                        if (otherRoom != null && !connectedRooms.contains(otherRoom)) {
                            connectedRooms.add(otherRoom);
                            Set<Room> otherConnectedRooms = roomConnections.get(otherRoom);
                            if (otherConnectedRooms == null) {
                                otherConnectedRooms = new HashSet<>();
                                roomConnections.put(otherRoom, otherConnectedRooms);
                            }
                            otherConnectedRooms.add(room);
                            tiles.put(vec, RoomTile.DOOR);
                        }
                    }
                }
            }
        }
        return new House(rooms, tiles, roomMap);
    }

    List<Room> splitRoom(Room room) {
        List<Room> result = new ArrayList<>();
        Orientation ori;
        int width = room.width();
        int height = room.height();
        if (width <= 6 || height <= 6) {
            result.add(room);
            return result;
        } else if (width < 10 && height < 10 && random.nextInt(3) == 0) {
            result.add(room);
            return result;
        } else if (width > height) {
            ori = Orientation.HORIZONTAL;
        } else if (height > width) {
            ori = Orientation.VERTICAL;
        } else if (random.nextBoolean()) {
            ori = Orientation.HORIZONTAL;
        } else {
            ori = Orientation.VERTICAL;
        }
        if (ori == Orientation.HORIZONTAL) {
            int split = 3 + random.nextInt(width - 6);
            result.addAll(splitRoom(new Room(room.ax, room.ay,
                                             room.ax + split, room.by)));
            result.addAll(splitRoom(new Room(room.ax + split, room.ay,
                                             room.bx, room.by)));
        } else {
            int split = 3 + random.nextInt(height - 6);
            result.addAll(splitRoom(new Room(room.ax, room.ay,
                                             room.bx, room.ay + split)));
            result.addAll(splitRoom(new Room(room.ax, room.ay + split,
                                             room.bx, room.by)));
        }
        return result;
    }

    boolean roomsConnected(List<Room> rooms) {
        if (rooms.size() <= 1) return true;
        List<Room> todo = new ArrayList<>();
        Set<Room> done = new HashSet<>();
        Set<Room> connected = new HashSet<>();
        todo.add(rooms.get(0));
        connected.add(rooms.get(0));
        while (!todo.isEmpty()) {
            Room room1 = todo.remove(0);
            done.add(room1);
            for (Room room2: rooms) {
                if (room1 != room2
                    && !done.contains(room2)) {
                    boolean conX = (room1.ax == room2.bx || room1.bx == room2.ax) && room1.ay <= room2.by && room1.by >= room2.ay;
                    boolean conY = (room1.ay == room2.by || room1.by == room2.ay) && room1.ax <= room2.bx && room1.bx >= room2.ax;
                    if (conX || conY) {
                        connected.add(room2);
                        todo.add(room2);
                    }
                }
            }
        }
        return connected.size() == rooms.size();
    }

    Block findHighestBlock(Block block) {
        Vec2 vec = new Vec2(block.getX(), block.getZ());
        Block result = highestBlocks.get(vec);
        if (result != null) return result;
        block = block.getWorld().getBlockAt(vec.x, 0, vec.y);
        while (block.getLightFromSky() == 0) block = block.getRelative(0, 1, 0);
        while (block.isLiquid()) block = block = block.getRelative(0, 1, 0);
        do { block = block.getRelative(0, -1, 0); } while (replaceMats.contains(block.getType()));
        highestBlocks.put(vec, block);
        return block;
    }

    @RequiredArgsConstructor
    static final class Room {
        public final int ax, ay, bx, by;
        public Cuboid boundingBox;

        int width() {
            return Math.abs(ax - bx) + 1;
        }

        int height() {
            return Math.abs(ay - by) + 1;
        }
    }

    @RequiredArgsConstructor
    static final class House {
        final List<Room> rooms;
        final Map<Vec2, RoomTile> tiles;
        final Map<Vec2, Room> roomMap;
        final List<Vec3> npcs = new ArrayList<>();
        public Cuboid boundingBox;
        int townId;
    }

    @RequiredArgsConstructor
    static final class Town {
        final int ax, ay, bx, by;
        final List<Vec2> chunks;
        final List<House> houses = new ArrayList<>();
        int townId;
    }

    enum RoomTile {
        WALL("|"), FLOOR("."), DOOR("+"), WINDOW("o"), DECORATION("T"), NPC("@");
        public final String stringIcon;
        RoomTile(String icon) {
            this.stringIcon = icon;
        }
        boolean isWall() {
            switch (this) {
            case WALL: case DOOR: case WINDOW: return true;
            default: return false;
            }
        }
    }

    enum Orientation {
        HORIZONTAL,
        VERTICAL;
    }

    enum Facing {
        NORTH(4, 2, 2),
        SOUTH(3, 3, 3),
        WEST(2, 4, 0),
        EAST(1, 5, 1);
        public final int dataTorch;
        public final int dataBlock;
        public final int dataStair;
        Facing(int dataTorch, int dataBlock, int dataStair) {
            this.dataTorch = dataTorch;
            this.dataBlock = dataBlock;
            this.dataStair = dataStair;
        }
    }

    enum Flag {
        COBBLE(Strategy.STYLE),
        SANDSTONE(Strategy.STYLE),
        RED_SANDSTONE(Strategy.STYLE),
        STONEBRICK(Strategy.STYLE),
        BRICKS(Strategy.STYLE),
        KOONTZY(Strategy.STYLE),
        WOOD(Strategy.STYLE),
        SPRUCE_WOOD(Strategy.STYLE),
        BIRCH_WOOD(Strategy.STYLE),
        JUNGLE_WOOD(Strategy.STYLE),
        ACACIA_WOOD(Strategy.STYLE),
        DARK_OAK_WOOD(Strategy.STYLE),
        CONCRETE(Strategy.STYLE),
        TERRACOTTA(Strategy.STYLE),
        QUARTZ(Strategy.STYLE),
        STONE(Strategy.STYLE),
        ANDESITE(Strategy.STYLE),
        DIORITE(Strategy.STYLE),
        GRANITE(Strategy.STYLE),
        PURPUR(Strategy.STYLE),
        NETHER_BRICK(Strategy.STYLE),

        NO_ROOF(Strategy.RANDOM),
        NO_BASE(Strategy.RANDOM),
        NO_DECORATION(Strategy.RANDOM),
        NO_NPCS(Strategy.RANDOM),

        VILLAGER(Strategy.NPC),
        UNDEAD(Strategy.NPC),

        ACACIA_DOOR(Strategy.DOOR),
        BIRCH_DOOR(Strategy.DOOR),
        DARK_OAK_DOOR(Strategy.DOOR),
        IRON_DOOR(Strategy.DOOR),
        JUNGLE_DOOR(Strategy.DOOR),
        SPRUCE_DOOR(Strategy.DOOR),
        OAK_DOOR(Strategy.DOOR),

        SURFACE(Strategy.ALTITUDE),
        UNDERGROUND(Strategy.ALTITUDE),
        FLOATING(Strategy.ALTITUDE),
        HERE(Strategy.ALTITUDE),

        RANDOM(Strategy.RANDOM);
        enum Strategy {
            ALTITUDE, STYLE, DOOR, NPC, RANDOM;
        }
        final Strategy strategy;
        Flag(Strategy strategy) {
            this.strategy = strategy;
        }
    }
}
