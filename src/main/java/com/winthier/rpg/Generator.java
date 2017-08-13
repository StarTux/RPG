package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;

final class Generator {
    static final Set<Material> replaceMats = EnumSet.of(Material.LOG, Material.LOG_2, Material.LEAVES, Material.LEAVES_2, Material.PUMPKIN, Material.HUGE_MUSHROOM_1, Material.HUGE_MUSHROOM_2, Material.SNOW, Material.ICE, Material.PACKED_ICE);

    final RPGPlugin plugin;
    final Random random = new Random(System.currentTimeMillis());
    final Map<Vec2, Block> highestBlocks = new HashMap<>();
    private Set<Flag> flags = EnumSet.noneOf(Flag.class);
    private Map<Flag.Strategy, Flag> uniqueFlags = new EnumMap<>(Flag.Strategy.class);
    // Transient
    private int roomHeight;
    private Style style;
    private Town town;
    private House house;

    Generator(RPGPlugin plugin) {
        this.plugin = plugin;
        for (Flag.Strategy strat: Flag.Strategy.values()) uniqueFlags.put(strat, Flag.RANDOM);
    }

    Generator() {
        this((RPGPlugin)null);
    }

    int randomInt(int i) {
        if (i < 2) return 0;
        return random.nextInt(i);
    }

    void setFlags(Collection<Flag> newFlags) {
        this.flags.clear();
        this.flags.addAll(newFlags);
        for (Flag.Strategy strat: Flag.Strategy.values()) uniqueFlags.put(strat, Flag.RANDOM);
        for (Flag flag: flags) {
            uniqueFlags.put(flag.strategy, flag);
        }
    }

    String generateName() {
        return generateName(1 + randomInt(3));
    }

    String generateName(int syllables) {
        final String[] beginSyllable = {"b", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "qu", "r", "s", "t", "v", "w", "x", "y", "z"};
        final String[] beginLong = {"gl", "gm", "gl", "gr", "sh", "sk", "sl", "sm", "sn", "sp", "st", "ch", "kr", "fr", "tr", "bl", "ph", "pl", "pr", "str", "chr", "schw", "th", "thr", "thn", "rh"};
        final String[] vocals = {"a", "e", "i", "o", "u"};
        final String[] longVocals = {"aa", "ee", "oo"};
        final String[] diphtongs = {"ae", "ai", "au", "ea", "ei", "ia", "ie", "io", "oa", "oi", "ou", "ua", "ui"};
        final String[] accents = {"á", "â", "à", "é", "ê", "è", "ó", "ô", "ò", "ú", "û", "ù"};
        final String[] umlauts = {"ä", "ö", "ü"};
        final String[] endSyllable = {"b", "d", "f", "g", "k", "l", "m", "n", "p", "r", "s", "t", "v", "w", "x", "y", "z"};
        final String[] endLong = {"gh", "ld", "lf", "lg", "lm", "ln", "ls", "lt", "mb", "mf", "mh", "mk", "mn", "mp", "ms", "mt", "mz", "nd", "ng", "nk", "rg", "rk", "rm", "rn", "rs", "rt", "sb", "sch", "st", "sd", "sh", "tsh", "ts", "rst", "tch", "ch", "th", "rth", "nth", "ns"};
        final String[] endStrong = {"ff", "gg", "kk", "ll", "mm", "nn", "pp", "rr", "ss", "tt", "tz", "ck"};
        StringBuilder sb = new StringBuilder();
        boolean priorHasEnd = true;
        boolean priorStrongEnd = false;
        boolean priorLongEnd = false;
        int useSpecialChars = randomInt(2);
        for (int i = 0; i < syllables; i += 1) {
            boolean hasBegin = priorHasEnd && !priorLongEnd && !priorStrongEnd ? random.nextInt(3) > 0 : true;
            boolean hasEnd = hasBegin ? random.nextInt(5) == 0 : true;
            if (hasBegin) {
                if (priorLongEnd || random.nextInt(beginSyllable.length + beginLong.length) < beginSyllable.length) {
                    sb.append(beginSyllable[randomInt(beginSyllable.length)]);
                } else {
                    sb.append(beginLong[randomInt(beginLong.length)]);
                }
            }
            boolean allowStrongEnd = true;
            switch (randomInt(6)) {
            case 0:
                switch (useSpecialChars) {
                case 0: sb.append(accents[randomInt(accents.length)]); break;
                case 1: default: sb.append(umlauts[randomInt(umlauts.length)]);
                }
                break;
            case 1: sb.append(longVocals[randomInt(longVocals.length)]);
                allowStrongEnd = false;
                break;
            case 2: sb.append(diphtongs[randomInt(diphtongs.length)]); break;
            default: sb.append(vocals[randomInt(vocals.length)]);
            }
            if (hasEnd) {
                if (!priorStrongEnd && allowStrongEnd && randomInt(5) == 0) {
                    sb.append(endStrong[randomInt(endStrong.length)]);
                    priorStrongEnd = true;
                    priorLongEnd = false;
                } else {
                    if (random.nextInt(endSyllable.length + endLong.length) < endSyllable.length) {
                        sb.append(endSyllable[randomInt(endSyllable.length)]);
                        priorStrongEnd = false;
                        priorLongEnd = false;
                    } else {
                        sb.append(endLong[randomInt(endLong.length)]);
                        priorStrongEnd = false;
                        priorLongEnd = true;
                    }
                }
            }
            priorHasEnd = hasEnd;
        }
        String result = sb.toString();
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    static String cleanSpecialChars(String string) {
        return string.replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("á", "a").replace("â", "a").replace("à", "a").replace("é", "e").replace("ê", "e").replace("è", "e").replace("ó", "o").replace("ô", "o").replace("ò", "o").replace("ú", "u").replace("û", "u").replace("ù", "u");
    }

    Town tryToPlantTown(World world, int sizeInChunks) {
        double size = world.getWorldBorder().getSize() * 0.5 - 128.0;
        Block blockMin = world.getWorldBorder().getCenter().add(-size, 0, -size).getBlock();
        Block blockMax = world.getWorldBorder().getCenter().add(size, 0, size).getBlock();
        int x = blockMin.getX() + randomInt(blockMax.getX() - blockMin.getX());
        int z = blockMin.getZ() + randomInt(blockMax.getZ() - blockMin.getZ());
        Chunk centerChunk = world.getBlockAt(x, 0, z).getChunk();
        return tryToPlantTown(centerChunk, sizeInChunks);
    }

    Town tryToPlantTown(Chunk centerChunk, int sizeInChunks) {
        World world = centerChunk.getWorld();
        double size = world.getWorldBorder().getSize() * 0.5;
        Chunk chunkMin = world.getWorldBorder().getCenter().add(-size, 0, -size).getBlock().getRelative(-1, 0, -1).getChunk();
        Chunk chunkMax = world.getWorldBorder().getCenter().add(size, 0, size).getBlock().getRelative(1, 0, 1).getChunk();
        List<Chunk> todo = new ArrayList<>();
        todo.add(centerChunk);
        Set<Chunk> done = new HashSet<>();
        List<Vec2> chunks = new ArrayList<>(sizeInChunks);
        List<Integer> allHeights = new ArrayList<>(16 * 16 * sizeInChunks);
        while (!todo.isEmpty() && chunks.size() < sizeInChunks) {
            Chunk chunk = todo.remove(0);
            if (done.contains(chunk)) continue;
            done.add(chunk);
            if (chunk.getX() <= chunkMin.getX() || chunk.getX() >= chunkMax.getX() ||
                chunk.getZ() <= chunkMin.getZ() || chunk.getZ() >= chunkMax.getZ()) continue;
            List<Integer> heights = new ArrayList<>(16 * 16);
            boolean fits = true;
            for (int z = 0; z < 16; z += 1) {
                for (int x = 0; x < 16; x += 1) {
                    Block block = findHighestBlock(chunk.getBlock(x, 0, z));
                    if (block.isLiquid()
                        || block.getType() == Material.ICE
                        || block.getType() == Material.PACKED_ICE) {
                        heights.add(0);
                        fits = false;
                        break;
                    } else {
                        heights.add(block.getY() + 1);
                    }
                }
                if (!fits) break;
            }
            if (!fits) continue;
            Collections.sort(heights);
            int median = heights.get(heights.size() / 2);
            for (int i: heights) {
                if (Math.abs(i - median) > 8) {
                    fits = false;
                    break;
                }
            }
            if (!fits) continue;
            List<Integer> newAllHeights = new ArrayList<>(allHeights);
            newAllHeights.addAll(heights);
            Collections.sort(newAllHeights);
            median = newAllHeights.get(newAllHeights.size() / 2);
            for (int i: heights) {
                if (Math.abs(i - median) > 16) {
                    fits = false;
                    break;
                }
            }
            if (!fits) continue;
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
            Collections.shuffle(todo, random);
        }
        if (chunks.size() < sizeInChunks) return null;
        int ax = chunks.get(0).x;
        int bx = chunks.get(0).x;
        int ay = chunks.get(0).y;
        int by = chunks.get(0).y;
        List<Vec2> borderChunks = new ArrayList<>();
        for (Vec2 chunk: chunks) {
            if (chunk.x < ax) ax = chunk.x;
            if (chunk.x > bx) bx = chunk.x;
            if (chunk.y < ay) ay = chunk.y;
            if (chunk.y > by) by = chunk.y;
            // final Vec2[] nbors = {chunk.relative(0, -1), chunk.relative(1, 0), chunk.relative(0, 1), chunk.relative(-1, 0),
            //                       chunk.relative(-1, -1), chunk.relative(1, -1), chunk.relative(1, 1), chunk.relative(-1, 1)};
            // for (Vec2 nbor: nbors) if (!chunks.contains(nbor)) borderChunks.add(nbor);
        }
        List<Vec2> wilderChunks = new ArrayList<>();
        for (int y = ay - 6; y <= by + 6; y += 1) {
            for (int x = ax - 6; x <= bx + 6; x += 1) {
                if (x >= ax - 1 && x <= by + 1 && y >= ay - 1 && y <= by - 1) {
                    wilderChunks.add(new Vec2(x, y));
                }
            }
        }
        return new Town(ax * 16, ay * 16, bx * 16 + 15, by * 16 + 15, chunks, wilderChunks);
    }

    void plantTown(World world, Town town) {
        this.town = town;
        Collections.shuffle(town.chunks);
        List<Vec2> roadBlocks = new ArrayList<>();
        for (Vec2 chunk: town.chunks) {
            if (town.chunks.contains(chunk.relative(1, 0))) {
                for (int z = 0; z < 16; z += 1) {
                    roadBlocks.add(new Vec2(chunk.x * 16 + 15, chunk.y * 16 + z));
                }
            }
            if (town.chunks.contains(chunk.relative(-1, 0))) {
                for (int z = 0; z < 16; z += 1) {
                    roadBlocks.add(new Vec2(chunk.x * 16, chunk.y * 16 + z));
                }
            }
            if (town.chunks.contains(chunk.relative(0, 1))) {
                for (int x = 0; x < 16; x += 1) {
                    roadBlocks.add(new Vec2(chunk.x * 16 + x, chunk.y * 16 + 15));
                }
            }
            if (town.chunks.contains(chunk.relative(0, -1))) {
                for (int x = 0; x < 16; x += 1) {
                    roadBlocks.add(new Vec2(chunk.x * 16 + x, chunk.y * 16));
                }
            }
        }
        for (Vec2 vec: roadBlocks) {
            List<Integer> ys = new ArrayList<>();
            for (Vec2 vec2: roadBlocks) {
                if (vec.maxDistance(vec2) <= 5) {
                    ys.add(findHighestBlock(world.getBlockAt(vec2.x, 0, vec2.y)).getY());
                }
            }
            Collections.sort(ys);
            Block block = world.getBlockAt(vec.x, ys.get(ys.size() / 2), vec.y);
            for (int i = 1; i <= 2; i += 1) block.getRelative(0, i, 0).setType(Material.AIR);
            Tile.of(Material.DIRT).setBlockNoPhysics(block.getRelative(0, -1, 0));
            Tile.of(Material.GRASS_PATH).setBlockNoPhysics(block);
        }
        int fountains = 1;
        int farms = 1 + randomInt(2);
        int pastures = randomInt(2);
        int mines = randomInt(2);
        for (Vec2 chunk: town.chunks) {
            Chunk bchunk = world.getChunkAt(chunk.x, chunk.y);
            for (Entity e: bchunk.getEntities()) {
                if (e instanceof LivingEntity && ((LivingEntity)e).getRemoveWhenFarAway()) e.remove();
            }
            if (fountains > 0) {
                fountains -= 1;
                int size = 4 + randomInt(2);
                int offx = 1 + randomInt(14 - size);
                int offy = 1 + randomInt(14 - size);
                town.structs.add(plantFountain(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), size, town));
            } else if (farms > 0) {
                farms -= 1;
                int width = 7 + randomInt(3) * 2;
                int height = 7 + randomInt(3) * 2;
                int offx = 1 + randomInt(14 - width);
                int offy = 1 + randomInt(14 - height);
                town.structs.add(plantFarm(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), width, height));
            } else if (pastures > 0) {
                pastures -= 1;
                int width = 8 + randomInt(7);
                int height = 8 + randomInt(7);
                int offx = 1 + randomInt(14 - width);
                int offy = 1 + randomInt(14 - height);
                town.structs.add(plantPasture(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), width, height));
            } else if (mines > 0) {
                mines -= 1;
                int width = 6 + randomInt(7);
                int height = 6 + randomInt(7);
                int offx = 1 + randomInt(14 - width);
                int offy = 1 + randomInt(14 - height);
                town.structs.add(plantMine(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), width, height));
            } else {
                int width = 10 + randomInt(5) - randomInt(3);
                int height = 10 + randomInt(5) - randomInt(3);
                int offx = width >= 13 ? 1 : 1 + randomInt(13 - width);
                int offy = height >= 13 ? 1 : 1 + randomInt(13 - height);
                House house = generateHouse(width, height);
                town.houses.add(house);
                town.structs.add(plantHouse(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), house));
            }
        }
        int lairs = 1;
        Collections.shuffle(town.wilderChunks, random);
        Vec2 centerChunk = new Vec2((town.ax + town.bx) / 2, (town.ay + town.by) / 2);
        Collections.sort(town.wilderChunks, (a, b) -> Integer.compare(b.maxDistance(centerChunk), a.maxDistance(centerChunk)));
        for (Vec2 chunk: town.wilderChunks) {
            int totalWild = lairs;
            if (totalWild <= 0) break;
            Chunk bchunk = world.getChunkAt(chunk.x, chunk.y);
            List<Integer> heights = new ArrayList<>(16 * 16);
            boolean fits = true;
            for (int z = 0; z < 16; z += 1) {
                for (int x = 0; x < 16; x += 1) {
                    Block block = findHighestBlock(bchunk.getBlock(x, 0, z));
                    if (block.isLiquid()
                        || block.getType() == Material.ICE
                        || block.getType() == Material.PACKED_ICE) {
                        heights.add(0);
                        fits = false;
                        break;
                    } else {
                        heights.add(block.getY() + 1);
                    }
                }
                if (!fits) break;
            }
            if (!fits) continue;
            Collections.sort(heights);
            int median = heights.get(heights.size() / 2);
            for (int i: heights) {
                if (Math.abs(i - median) > 4) {
                    fits = false;
                    break;
                }
            }
            if (!fits) continue;
            if (lairs > 0) {
                lairs -= 1;
                EnumSet<Flag> oldFlags = EnumSet.copyOf(flags);
                House house = generateHouse(16, 16);
                town.structs.add(plantLair(world.getBlockAt(chunk.x * 16, 0, chunk.y * 16), house, town));
            }
        }
    }

    Struct plantHouse(Block start, House house) {
        this.house = house;
        Flag flagAltitude = uniqueFlags.get(Flag.Strategy.ALTITUDE);
        Map<Vec2, RoomTile> tiles = house.tiles;
        boolean noRoof = flags.contains(Flag.NO_ROOF) || flagAltitude == Flag.UNDERGROUND;
        boolean noBase = flags.contains(Flag.NO_BASE) || flagAltitude == Flag.FLOATING;
        boolean noDecoration = flags.contains(Flag.NO_DECORATION);
        int floorLevel;
        switch (flagAltitude) {
        case UNDERGROUND:
            floorLevel = 4 + randomInt(40);
            break;
        case FLOATING:
            floorLevel = 128 + randomInt(100);
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
        Block offset = start.getWorld().getBlockAt(start.getX(), floorLevel, start.getZ());
        int color = randomInt(16);
        roomHeight = 4 + randomInt(3);
        int windowHeight;
        switch (uniqueFlags.get(Flag.Strategy.PURPOSE)) {
        case SHOP: windowHeight = roomHeight - 2; break;
        default: windowHeight = 1 + randomInt(roomHeight - 3);
        }
        house.offset = new Vec3(offset.getX(), offset.getY(), offset.getZ());
        {
            int ax, az, bx, bz;
            ax = az = Integer.MAX_VALUE;
            bx = bz = Integer.MIN_VALUE;
            int ay = floorLevel;
            int by = floorLevel + roomHeight;
            for (Room room: house.rooms) {
                Cuboid bb = new Cuboid(room.ax + offset.getX(), ay, room.ay + offset.getZ(),
                                       room.bx + offset.getX(), by, room.by + offset.getZ());
                room.boundingBox = bb;
                if (ax > bb.ax) ax = bb.ax;
                if (az > bb.az) az = bb.az;
                if (bx < bb.bx) bx = bb.bx;
                if (bz < bb.bz) bz = bb.bz;
            }
            house.boundingBox = new Cuboid(ax, ay, az, bx, by, bz);
        }
        Flag flagDoor = uniqueFlags.get(Flag.Strategy.DOOR);
        Flag flagStyle = uniqueFlags.get(Flag.Strategy.STYLE);
        style = new Style(flagStyle, color);
        Material matDoor;
        switch (flagDoor) {
        case RANDOM:
            switch (flagStyle) {
            case OAK: matDoor = Material.WOODEN_DOOR; break;
            case SPRUCE: matDoor = Material.SPRUCE_DOOR; break;
            case BIRCH: matDoor = Material.BIRCH_DOOR; break;
            case JUNGLE: matDoor = Material.JUNGLE_DOOR; break;
            case ACACIA: matDoor = Material.ACACIA_DOOR; break;
            case DARK_OAK: matDoor = Material.DARK_OAK_DOOR; break;
            case NETHER: matDoor = Material.IRON_DOOR_BLOCK; break;
            case IRON: matDoor = Material.IRON_DOOR_BLOCK; break;
            default:
                switch (randomInt(7)) {
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
        for (Vec2 vec: tiles.keySet()) {
            Block floor = offset.getRelative(vec.x, 0, vec.y);
            RoomTile tile = tiles.get(vec);
            RoomTile tileEast = tiles.get(vec.relative(1, 0));
            RoomTile tileSouth = tiles.get(vec.relative(0, 1));
            RoomTile tileWest = tiles.get(vec.relative(-1, 0));
            RoomTile tileNorth = tiles.get(vec.relative(0, -1));
            boolean tileIsWall = tile != null && tile.isWall();
            boolean eastIsWall = tileEast != null && tileEast.isWall();
            boolean southIsWall = tileSouth != null && tileSouth.isWall();
            boolean westIsWall = tileWest != null && tileWest.isWall();
            boolean northIsWall = tileNorth != null && tileNorth.isWall();
            boolean tileIsCorner = tileIsWall && ((eastIsWall && southIsWall) || (southIsWall && westIsWall) || (westIsWall && northIsWall) || (northIsWall && eastIsWall));
            Orientation ori = eastIsWall || westIsWall ? Orientation.HORIZONTAL : Orientation.VERTICAL;
            // Make a base
            if (!noBase) {
                Block block = floor.getRelative(0, -1, 0);
                while (true) {
                    Material mat = block.getType();
                    boolean doIt = !mat.isSolid() || replaceMats.contains(mat);
                    if (!doIt) break;
                    if (tileIsCorner) {
                        style.corner.setBlock(block);
                    } else {
                        style.foundation.setBlock(block);
                    }
                    block = block.getRelative(0, -1, 0);
                }
            }
            int randomWallCount;
            if (style.randomWallChance < 0.0) {
                randomWallCount = 0;
            } else if (style.randomWallChance == 0) {
                randomWallCount = 1;
            } else {
                randomWallCount = (int)((double)(roomHeight - style.baseLevel - 2) * style.randomWallChance) + 1;
            }
            List<Integer> randomWalls = new ArrayList<>();
            if (randomWallCount > 0) {
                for (int i = style.baseLevel + 1; i < roomHeight; i += 1) randomWalls.add(i);
                Collections.shuffle(randomWalls, random);
                while (randomWalls.size() > randomWallCount) randomWalls.remove(randomWalls.size() - 1);
            }
            switch (tile) {
            case FLOOR:
                for (int i = 0; i <= roomHeight; i += 1) {
                    if (i == 0) {
                        if ((vec.x & 1) == 0 ^ (vec.y & 1) == 0) {
                            style.floor.setBlock(floor.getRelative(0, i, 0));
                        } else {
                            style.floorAlt.setBlock(floor.getRelative(0, i, 0));
                        }
                    } else if (i == roomHeight) {
                        if ((vec.x & 1) == 0 ^ (vec.y & 1) == 0) {
                            style.ceiling.setBlock(floor.getRelative(0, i, 0));
                        } else {
                            style.ceilingAlt.setBlock(floor.getRelative(0, i, 0));
                        }
                    } else {
                        Tile.AIR.setBlock(floor.getRelative(0, i, 0));
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
                for (int i = 0; i <= roomHeight; i += 1) {
                    if (i == 0) {
                        if (style.baseLevel == 0) {
                            style.wallBase.orient(ori).setBlock(floor.getRelative(0, i, 0));
                        } else {
                            style.foundation.setBlock(floor.getRelative(0, i, 0));
                        }
                    } else if (i == 1) {
                        floor.getRelative(0, i, 0).setTypeIdAndData(matDoor.getId(), (byte)dataDoor, false);
                    } else if (i == 2) {
                        floor.getRelative(0, i, 0).setTypeIdAndData(matDoor.getId(), (byte)0x8, false);
                    } else if (i == roomHeight) {
                        style.wallTop.orient(ori).setBlock(floor.getRelative(0, i, 0));
                    } else if (randomWalls.contains(i)) {
                        style.wallRandom.setBlock(floor.getRelative(0, i, 0));
                    } else {
                        style.wall.setBlock(floor.getRelative(0, i, 0));
                    }
                }
                break;
            case WINDOW:
                for (int i = 0; i <= roomHeight; i += 1) {
                    if (i == style.baseLevel) {
                        style.wallBase.orient(ori).setBlock(floor.getRelative(0, i, 0));
                    } else if (i == 0) {
                        style.foundation.setBlock(floor.getRelative(0, i, 0));
                    } else if (i >= 2 && i < 2 + windowHeight) {
                        style.window.setBlock(floor.getRelative(0, i, 0));
                    } else if (i == roomHeight) {
                        style.wallTop.orient(ori).setBlock(floor.getRelative(0, i, 0));
                    } else if (randomWalls.contains(i)) {
                        style.wallRandom.setBlock(floor.getRelative(0, i, 0));
                    } else {
                        style.wall.setBlock(floor.getRelative(0, i, 0));
                    }
                }
                break;
            case WALL:
            default:
                if (tileIsCorner) {
                    for (int i = 0; i <= roomHeight; i += 1) {
                        if (i == style.baseLevel) {
                            style.cornerBase.setBlock(floor.getRelative(0, i, 0));
                        } else if (i == roomHeight) {
                            style.cornerTop.setBlock(floor.getRelative(0, i, 0));
                        } else {
                            style.corner.setBlock(floor.getRelative(0, i, 0));
                        }
                    }
                } else {
                    for (int i = 0; i <= roomHeight; i += 1) {
                        style.wallTop.orient(ori).setBlock(floor.getRelative(0, i, 0));
                        if (i == style.baseLevel) {
                            style.wallBase.orient(ori).setBlock(floor.getRelative(0, i, 0));
                        } else if (i == 0) {
                            style.foundation.setBlock(floor.getRelative(0, i, 0));
                        } else if (i == roomHeight) {
                            style.wallTop.orient(ori).setBlock(floor.getRelative(0, i, 0));
                        } else if (randomWalls.contains(i)) {
                            style.wallRandom.setBlock(floor.getRelative(0, i, 0));
                        } else {
                            style.wall.setBlock(floor.getRelative(0, i, 0));
                        }
                    }
                }
            }
        }
        // Decorations
        if (!noDecoration) {
            int beds = 1 + randomInt(house.rooms.size());
            for (Room room: house.rooms) {
                boolean bed = false;
                int amount = room.width() + room.height();
                int torches = 1 + randomInt(2);
                Set<Vec2> decs = new HashSet<>();
                int ax = room.ax + 1;
                int ay = room.ay + 1;
                int bx = room.bx - 1;
                int by = room.by - 1;
                if (room.width() <= 4) {
                    if (random.nextBoolean()) {
                        ax += 1;
                    } else {
                        bx -= 1;
                    }
                }
                if (room.height() <= 4) {
                    if (random.nextBoolean()) {
                        ay += 1;
                    } else {
                        by -= 1;
                    }
                }
                for (int x = ax; x <= bx; x += 1) {
                    decs.add(new Vec2(x, ay));
                    decs.add(new Vec2(x, by));
                }
                for (int y = ay; y <= by; y += 1) {
                    decs.add(new Vec2(ax, y));
                    decs.add(new Vec2(bx, y));
                }
                // remove door blockers
                for (int x = room.ax + 1; x < room.bx; x += 1) {
                    if (tiles.get(new Vec2(x, room.ay)) == RoomTile.DOOR) {
                        decs.remove(new Vec2(x, room.ay + 1));
                        decs.remove(new Vec2(x, room.ay + 2));
                    }
                    if (tiles.get(new Vec2(x, room.by)) == RoomTile.DOOR) {
                        decs.remove(new Vec2(x, room.by - 1));
                        decs.remove(new Vec2(x, room.by - 2));
                    }
                }
                for (int y = room.ay + 1; y < room.by; y += 1) {
                    if (tiles.get(new Vec2(room.ax, y)) == RoomTile.DOOR) {
                        decs.remove(new Vec2(room.ax + 1, y));
                        decs.remove(new Vec2(room.ax + 2, y));
                    }
                    if (tiles.get(new Vec2(room.bx, y)) == RoomTile.DOOR) {
                        decs.remove(new Vec2(room.bx - 1, y));
                        decs.remove(new Vec2(room.bx - 2, y));
                    }
                }
                house.decorationList = new ArrayList<>(decs);
                Collections.shuffle(house.decorationList, random);
                for (Vec2 vec: house.decorationList) {
                    if (tiles.get(vec) != RoomTile.FLOOR) continue;
                    RoomTile[] nbors = {
                        tiles.get(vec.relative(0, -1)),
                        tiles.get(vec.relative(1, 0)),
                        tiles.get(vec.relative(0, 1)),
                        tiles.get(vec.relative(-1, 0)) };
                    boolean skip = false;
                    Facing facing = Facing.NORTH;
                    boolean window = false;
                    for (int j = 0; j < 4; j += 1) {
                        RoomTile nbor = nbors[j];
                        if (nbor == null) {
                            continue;
                        } else if (nbor == RoomTile.DOOR) {
                            skip = true;
                            break;
                        } else if (nbor == RoomTile.WINDOW) {
                            window = true;
                        }
                        if (nbor.isWall()) {
                            switch (j) {
                            case 0: facing = Facing.SOUTH; break;
                            case 1: facing = Facing.WEST; break;
                            case 2: facing = Facing.NORTH; break;
                            case 3: default: facing = Facing.EAST;
                            }
                        }
                    }
                    if (skip) continue;
                    Block block = offset.getRelative(vec.x, 1, vec.y);
                    house.tiles.put(vec, RoomTile.DECORATION);
                    if (!bed && beds > 0 && house.decorationList.contains(vec.relative(facing.rotate().vector)) && tiles.get(vec.relative(facing.rotate().vector)) == RoomTile.FLOOR) {
                        bed = true;
                        beds -= 1;
                        placeDecoration(Decoration.BED, block, vec, facing, window);
                    } else if (torches > 0) {
                        torches -= 1;
                        placeDecoration(Decoration.TORCH, block, vec, facing, window);
                    } else {
                        Decoration decoration = Decoration.values()[randomInt(Decoration.values().length)];
                        placeDecoration(decoration, block, vec, facing, window);
                    }
                }
                int carpetX = randomInt(room.width() - 3);
                int carpetY = randomInt(room.height() - 4);
                if (carpetX > 0 && carpetY > 0) {
                    int offx = 2 + randomInt(room.width() - 3 - carpetX);
                    int offy = 2 + randomInt(room.height() - 3 - carpetY);
                    Tile tile = Tile.of(Material.CARPET, color);
                    int cax = room.ax + offx;
                    int cay = room.ay + offy;
                    int cbx = cax + carpetX - 1;
                    int cby = cay + carpetY - 1;
                    for (int y = cay; y <= cby; y += 1) {
                        for (int x = cax; x <= cbx; x += 1) {
                            Block block = offset.getRelative(x, 1, y);
                            if (block.getType() == Material.AIR) {
                                tile.setBlock(block);
                            }
                        }
                    }
                }
            }
        }
        // Make a roof
        if (!noRoof) {
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
            }
            boolean useStairs = random.nextBoolean();
            for (Vec2 vec: roofs.keySet()) {
                int roofLevel = roofs.get(vec);
                Block roof1 = offset.getRelative(vec.x, roomHeight + 1, vec.y);
                Block roof2 = roof1.getRelative(0, useStairs ? roofLevel : roofLevel / 2, 0);
                RoomTile tile = tiles.get(vec);
                if (tile != null) {
                    while (roof1.getY() < roof2.getY()) {
                        style.roofDoubleSlab.setBlock(roof1);
                        roof1 = roof1.getRelative(0, 1, 0);
                    }
                }
                if (useStairs) {
                    Integer nbor1 = roofs.get(vec.relative(relx, rely));
                    Integer nbor2 = roofs.get(vec.relative(-relx, -rely));
                    if (nbor1 == null) nbor1 = -1;
                    if (nbor2 == null) nbor2 = -1;
                    if (nbor1 < roofLevel && nbor2 < roofLevel) {
                        style.roofSlab.setBlock(roof2);
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
                        style.roofStair.or(dataStair).setBlock(roof2);
                    }
                } else {
                    if ((roofLevel & 1) == 0) {
                        style.roofSlab.setBlock(roof2);
                    } else {
                        if (tiles.containsKey(vec)) {
                            style.roofDoubleSlab.setBlock(roof2);
                        } else {
                            style.roofSlab.or(8).setBlock(roof2);
                        }
                    }
                }
            }
        }
        // Generate NPCs
        int totalNPCs = 1;
        for (int i = 0; i < house.rooms.size() - 1; i += 1) totalNPCs += randomInt(2);
        List<Vec2> possibleNPCSpots = new ArrayList<>();
        for (Room room: house.rooms) {
            List<Vec2> vecs = new ArrayList<>();
            for (int x = room.ax + 1; x < room.bx; x += 1) {
                for (int y = room.ay + 1; y < room.by; y += 1) {
                    Vec2 vec = new Vec2(x, y);
                    if (house.tiles.get(vec) == RoomTile.FLOOR) vecs.add(vec);
                }
            }
            if (!vecs.isEmpty()) possibleNPCSpots.add(vecs.get(randomInt(vecs.size())));
        }
        Collections.shuffle(possibleNPCSpots, random);
        totalNPCs = Math.min(possibleNPCSpots.size(), totalNPCs);
        for (int i = 0; i < totalNPCs; i += 1) {
            Vec2 vec = possibleNPCSpots.get(i);
            house.tiles.put(vec, RoomTile.NPC);
            house.npcs.add(house.offset.relative(vec.x, 1, vec.y));
        }
        return new Struct(Struct.Type.HOUSE, house.boundingBox,
                          house.rooms.stream().map(r -> new Struct(Struct.Type.ROOM, r.boundingBox, null, null)).collect(Collectors.toList()),
                          null);
    }

    /**
     * Must be set: house, style
     */
    boolean placeDecoration(Decoration decoration, Block block, Vec2 vec, Facing facing, boolean window) {
        switch (decoration) {
        case TORCH:
            style.torch.facing(facing).setBlockNoPhysics(block.getRelative(0, (window ? 0 : 1), 0));
            break;
        case BOOKSHELF:
            if (!window) {
                block.getRelative(0, 2, 0).setType(Material.BOOKSHELF);
                block.getRelative(0, 1, 0).setType(Material.BOOKSHELF);
            }
            block.setType(Material.BOOKSHELF);
            break;
        case CAULDRON:
            block.setType(Material.CAULDRON);
            break;
        case BREWING_STAND:
            block.setType(Material.BREWING_STAND);
            break;
        case JUKEBOX:
            block.setType(Material.JUKEBOX);
            break;
        case WORKBENCH:
            block.setType(Material.WORKBENCH);
            break;
        case FURNACE:
            block.setTypeIdAndData(Material.FURNACE.getId(), (byte)facing.dataBlock, true);
            break;
        case CHEST:
            block.setTypeIdAndData(Material.CHEST.getId(), (byte)facing.dataBlock, true);
            break;
        case CHAIR:
            style.stair.facing(facing).setBlock(block);
            break;
        case FLOWER_POT:
            style.stair.or(facing.dataStair | 4).setBlock(block);
            block.getRelative(0, 1, 0).setType(Material.FLOWER_POT);
            MaterialData flower;
            switch (randomInt(32)) {
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
            org.bukkit.block.FlowerPot pot = (org.bukkit.block.FlowerPot)block.getRelative(0, 1, 0).getState();
            pot.setContents(flower);
            pot.update();
            break;
        case ANVIL:
            block.setTypeIdAndData(Material.ANVIL.getId(), (byte)randomInt(12), true);
            break;
        case BANNER:
            block.getRelative(0, 1, 0).setTypeIdAndData(Material.WALL_BANNER.getId(), (byte)facing.dataBlock, true);
            org.bukkit.block.Banner banner = (org.bukkit.block.Banner)block.getRelative(0, 1, 0).getState();
            banner.setBaseColor(DyeColor.values()[style.color]);
            int patternCount = 1 + randomInt(4);
            List<Pattern> patterns = new ArrayList<>(patternCount);
            for (int j = 0; j < patternCount; j += 1) {
                patterns.add(new Pattern(DyeColor.values()[randomInt(DyeColor.values().length)], PatternType.values()[randomInt(PatternType.values().length)]));
            }
            banner.setPatterns(patterns);
            banner.update();
            break;
        case TABLE:
            block.setType(Material.FENCE);
            block.getRelative(0, 1, 0).setType(Material.WOOD_PLATE);
            break;
        case ENCHANTMENT_TABLE:
            Tile.of(Material.ENCHANTMENT_TABLE).setBlock(block);
            break;
        case ENDER_CHEST:
            Tile.of(Material.ENDER_CHEST, facing.dataBlock).setBlock(block);
            break;
        case LADDER:
            if (!window) {
                for (int i = 1; i < roomHeight; i += 1) {
                    Tile.of(Material.LADDER, facing.dataBlock).setBlock(block.getRelative(0, i, 0));
                }
            }
            Tile.of(Material.LADDER, facing.dataBlock).setBlock(block);
            break;
        case BED:
            Vec2 nbor = vec.relative(facing.rotate().vector);
            if (house.decorationList.contains(nbor) && house.tiles.get(nbor) == RoomTile.FLOOR) {
                house.tiles.put(nbor, RoomTile.DECORATION);
                Facing faceDown = facing.rotate();
                Facing faceUp = faceDown.opposite();
                Block blockHead = block;
                Block blockBottom = block.getRelative(faceDown.vector.x, 0, faceDown.vector.y);
                Tile.of(Material.BED_BLOCK).or(faceUp.dataBed | 8).setBlockNoPhysics(blockHead);
                Tile.of(Material.BED_BLOCK).or(faceUp.dataBed).setBlockNoPhysics(blockBottom);
                DyeColor bedColor = DyeColor.values()[style.color];
                org.bukkit.block.Bed bedState;
                bedState = (org.bukkit.block.Bed)blockHead.getState();
                bedState.setColor(bedColor);
                bedState.update();
                bedState = (org.bukkit.block.Bed)blockBottom.getState();
                bedState.setColor(bedColor);
                bedState.update();
            } else {
                return false;
            }
            break;
        default: return false;
        }
        return true;
    }

    Struct plantFountain(Block start, int size, Town town) {
        List<Integer> highest = new ArrayList<>();
        Map<Vec2, Integer> tiles = new HashMap<>();
        for (int y = 0; y < size; y += 1) {
            for (int x = 0; x < size; x += 1) {
                highest.add(findHighestBlock(start.getRelative(x, 0, y)).getY());
            }
        }
        Collections.sort(highest);
        int floorLevel = highest.get(highest.size() / 2);
        Block offset = start.getWorld().getBlockAt(start.getX(), floorLevel, start.getZ());
        style = new Style(uniqueFlags.get(Flag.Strategy.STYLE), random.nextInt(16));
        int height = 4 + randomInt(3);
        boolean nether = uniqueFlags.get(Flag.Strategy.STYLE) == Flag.NETHER;
        int waterLevel = 1 + randomInt(14);
        for (int z = 0; z < size; z += 1) {
            for (int x = 0; x < size; x += 1) {
                boolean outerX, outerZ, isWall, isCorner;
                outerX = x == 0 || x == size - 1;
                outerZ = z == 0 || z == size - 1;
                isCorner = outerX && outerZ;
                Orientation ori = outerX ? Orientation.VERTICAL : Orientation.HORIZONTAL;
                isWall = !isCorner && (outerX || outerZ);
                Block block = offset.getRelative(x, 0, z);
                if (isCorner) {
                    style.cornerTop.orient(ori).setBlock(block.getRelative(0, 1, 0));
                    for (int i = 2; i <= height; i += 1) {
                        if (i < height) {
                            style.pillar.setBlock(block.getRelative(0, i, 0));
                        } else {
                            style.slab.setBlock(block.getRelative(0, i, 0));
                        }
                    }
                    for (int i = 0; i < 4; i += 1) style.corner.setBlock(block.getRelative(0, -i, 0));
                } else if (isWall) {
                    style.wallTop.orient(ori).setBlock(block.getRelative(0, 1, 0));
                    for (int i = 2; i <= height; i += 1) {
                        if (i < height) {
                            Tile.AIR.setBlock(block.getRelative(0, i, 0));
                        } else {
                            style.slab.setBlock(block.getRelative(0, i, 0));
                        }
                    }
                    for (int i = 0; i < 4; i += 1) style.wall.setBlock(block.getRelative(0, -i, 0));
                } else {
                    for (int i = 1; i <= height; i += 1) {
                        if (i < height) {
                            Tile.AIR.setBlock(block.getRelative(0, i, 0));
                        } else {
                            style.slab.setBlock(block.getRelative(0, i, 0));
                        }
                    }
                    for (int i = 0; i < 16; i += 1) {
                        Block blockWater = block.getRelative(0, -i, 0);
                        if (i < waterLevel) {
                            blockWater.setType(Material.AIR);
                        } else {
                            if (nether) {
                                blockWater.setType(Material.STATIONARY_LAVA);
                            } else {
                                blockWater.setType(Material.STATIONARY_WATER);
                            }
                        }
                    }
                }
            }
        }
        int signX, signY;
        Facing facing;
        if (random.nextBoolean()) {
            signX = size / 2;
            if (random.nextBoolean()) {
                signY = -1;
                facing = Facing.NORTH;
            } else {
                signY = size;
                facing = Facing.SOUTH;
            }
        } else {
            signY = size / 2;
            if (random.nextBoolean()) {
                signX = -1;
                facing = Facing.WEST;
            } else {
                signX = size;
                facing = Facing.EAST;
            }
        }
        Block signBlock = offset.getRelative(signX, 1, signY);
        if (signBlock.getType().isSolid()) {
            signBlock = offset.getRelative(signX, height, signY);
        }
        Tile.of(Material.WALL_SIGN, facing.dataBlock).setBlockNoPhysics(signBlock);
        org.bukkit.block.Sign sign = (org.bukkit.block.Sign)signBlock.getState();
        String underscore;
        switch (randomInt(9)) {
        case 0: underscore = "============"; break;
        case 1: underscore = "------------"; break;
        case 2: underscore = "~~~~~~~~~~~~"; break;
        case 3: underscore = "~-~-~-~-~-~-~"; break;
        case 4: underscore = "////////////"; break;
        case 5: underscore = "o-o-o-o-o-o-o"; break;
        case 6: underscore = "! ! ! ! ! ! !"; break;
        case 7: underscore = "- - - - - - -"; break;
        default: underscore = ""; break;
        }
        sign.setLine(0, underscore);
        if (plugin != null) sign.setLine(1, plugin.getMessages().deal(Messages.Type.TOWN_SIGN));
        if (town != null) sign.setLine(2, town.name);
        sign.setLine(3, underscore);
        sign.update();
        Cuboid bb = new Cuboid(offset.getX(), offset.getY() - 4, offset.getZ(),
                               offset.getX() + size, offset.getY() + height, offset.getZ() + size);
        return new Struct(Struct.Type.FOUNTAIN, bb, null, null);
    }

    Struct plantFarm(Block start, int width, int height) {
        List<Integer> highest = new ArrayList<>();
        Map<Vec2, Integer> tiles = new HashMap<>();
        for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                highest.add(findHighestBlock(start.getRelative(x, 0, y)).getY());
            }
        }
        Collections.sort(highest);
        int floorLevel = highest.get(highest.size() / 2);
        Block offset = start.getWorld().getBlockAt(start.getX(), floorLevel, start.getZ());
        style = new Style(uniqueFlags.get(Flag.Strategy.STYLE), random.nextInt(16));
        boolean nether = uniqueFlags.get(Flag.Strategy.STYLE) == Flag.NETHER;
        Tile fruit, soil;
        Struct.Tag cropTag;
        if (nether) {
            fruit = Tile.of(Material.NETHER_WARTS, 3);
            soil = Tile.of(Material.SOUL_SAND);
            cropTag = Struct.Tag.NETHER_WART;
        } else {
            switch (randomInt(5)) {
            case 0:
                fruit = Tile.of(Material.BEETROOT_BLOCK, 3);
                cropTag = Struct.Tag.BEETROOT;
                break;
            case 1: fruit = Tile.of(Material.CARROT, 7);
                cropTag = Struct.Tag.CARROT;
                break;
            case 2: fruit = Tile.of(Material.POTATO, 7);
                cropTag = Struct.Tag.POTATO;
                break;
            case 3: default:
                fruit = Tile.of(Material.CROPS, 7);
                cropTag = Struct.Tag.WHEAT;
            }
            soil = Tile.of(Material.SOIL, 7);
        }
        int cx = width / 2;
        int cy = height / 2;
        for (int z = 0; z < height; z += 1) {
            for (int x = 0; x < width; x += 1) {
                boolean outerX, outerZ, isWall, isCorner;
                outerX = x == 0 || x == width - 1;
                outerZ = z == 0 || z == height - 1;
                isCorner = outerX && outerZ;
                Orientation ori = outerX ? Orientation.VERTICAL : Orientation.HORIZONTAL;
                isWall = !isCorner && (outerX || outerZ);
                Tile tile, tileBelow, tileAbove;
                boolean isCenter = false;
                if (isCorner) {
                    tile = style.cornerTop.orient(ori);
                    tileBelow = style.corner;
                    tileAbove = style.fence;
                } else if (isWall) {
                    tile = style.wallTop.orient(ori);
                    tileBelow = style.wall;
                    if (x != cx && z != cy) {
                        tileAbove = style.fence;
                    } else {
                        tileAbove = Tile.AIR;
                    }
                } else if (x == cx && z == cy && !nether) {
                    tile = Tile.of(Material.STATIONARY_WATER);
                    tileBelow = style.foundation;
                    tileAbove = style.lamp;
                    isCenter = true;
                } else {
                    tile = soil;
                    tileBelow = style.foundation;
                    tileAbove = fruit;
                }
                Block block = offset.getRelative(x, 0, z);
                Block foundation = block.getRelative(0, -1, 0);
                while (!foundation.getType().isSolid()) {
                    tileBelow.setBlock(foundation);
                    foundation = foundation.getRelative(0, -1, 0);
                }
                for (int i = 1; i <= 3; i += i) block.getRelative(0, i, 0).setType(Material.AIR);
                tile.setBlockNoPhysics(block);
                tileAbove.setBlockNoPhysics(block.getRelative(0, 1, 0));
                if (isCenter) {
                    style.slab.setBlock(block.getRelative(0, 2, 0));
                }
            }
        }
        Cuboid bb = new Cuboid(offset.getX(), offset.getY(), offset.getZ(),
                               offset.getX() + width - 1, offset.getY() + 3, offset.getZ() + height - 1);
        Cuboid bb2 = new Cuboid(offset.getX() + 1, offset.getY() + 1, offset.getZ() + 1,
                                offset.getX() + width - 2, offset.getY() + 1, offset.getZ() + height - 2);
        return new Struct(Struct.Type.FARM, bb,
                          Arrays.asList(new Struct(Struct.Type.CROPS, bb2, null, null)),
                          EnumSet.of(cropTag));
    }

    Struct plantPasture(Block start, int width, int height) {
        List<Integer> highest = new ArrayList<>();
        Map<Vec2, Integer> tiles = new HashMap<>();
        for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                highest.add(findHighestBlock(start.getRelative(x, 0, y)).getY());
            }
        }
        Collections.sort(highest);
        int floorLevel = highest.get(highest.size() / 2);
        Block offset = start.getWorld().getBlockAt(start.getX(), floorLevel, start.getZ());
        style = new Style(uniqueFlags.get(Flag.Strategy.STYLE), random.nextInt(16));
        boolean nether = uniqueFlags.get(Flag.Strategy.STYLE) == Flag.NETHER;
        int cx = width / 2;
        int cy = height / 2;
        List<Block> animalBlocks = new ArrayList<>();
        for (int z = 0; z < height; z += 1) {
            for (int x = 0; x < width; x += 1) {
                Facing facing;
                if (x == 0) {
                    facing = Facing.WEST;
                } else if (x == width - 1) {
                    facing = Facing.EAST;
                } else if (z == 0) {
                    facing = Facing.NORTH;
                } else {
                    facing = Facing.SOUTH;
                }
                boolean outerX, outerZ, isWall, isCorner;
                outerX = x == 0 || x == width - 1;
                outerZ = z == 0 || z == height - 1;
                isCorner = outerX && outerZ;
                Orientation ori = outerX ? Orientation.VERTICAL : Orientation.HORIZONTAL;
                isWall = !isCorner && (outerX || outerZ);
                Tile tile, tileBelow, tileAbove;
                Block block = offset.getRelative(x, 0, z);
                boolean isGate = false;
                if (isCorner) {
                    tile = style.cornerTop.orient(ori);
                    tileBelow = style.cornerBase;
                    tileAbove = style.corner;
                } else if (isWall) {
                    tile = style.wallTop.orient(ori);
                    tileBelow = style.wall;
                    if (x == cx || z == cy) {
                        tileAbove = style.gate.facing(facing);
                        isGate = true;
                    } else {
                        tileAbove = style.fence;
                    }
                } else {
                    tile = Tile.GRASS;
                    tileBelow = style.foundation;
                    tileAbove = Tile.AIR;
                    if (x > 1 && x < width - 2 && z > 1 && z < height - 2) {
                        animalBlocks.add(block.getRelative(0, 1, 0));
                    }
                }
                Block foundation = block.getRelative(0, -1, 0);
                while (!foundation.getType().isSolid()) {
                    tileBelow.setBlock(foundation);
                    foundation = foundation.getRelative(0, -1, 0);
                }
                for (int i = 1; i <= 3; i += 1) block.getRelative(0, i, 0).setType(Material.AIR);
                tile.setBlock(block);
                tileAbove.setBlock(block.getRelative(0, 1, 0));
                if (isCorner) {
                    style.lamp.setBlock(block.getRelative(0, 2, 0));
                    style.slab.setBlock(block.getRelative(0, 3, 0));
                } else if (isWall && !isGate && !style.fence.isTallFence()) {
                    style.fence.setBlock(block.getRelative(0, 2, 0));
                }
            }
        }
        List<String> tags = null;
        Struct.Tag entityTag;
        EntityType entityType;
        if (uniqueFlags.get(Flag.Strategy.STYLE) == Flag.NETHER) {
            switch (randomInt(2)) {
            case 0:
                entityType = EntityType.ZOMBIE_HORSE;
                entityTag = Struct.Tag.ZOMBIE_HORSE;
                break;
            case 1: default:
                entityType = EntityType.SKELETON_HORSE;
                entityTag = Struct.Tag.SKELETON_HORSE;
            }
        } else {
            switch (randomInt(16)) {
            case 0: case 1: case 2:
                entityType = EntityType.COW;
                entityTag = Struct.Tag.COW;
                break;
            case 3: case 4: case 5:
                entityType = EntityType.PIG;
                entityTag = Struct.Tag.PIG;
                break;
            case 6: case 7: case 8:
                entityType = EntityType.SHEEP;
                entityTag = Struct.Tag.SHEEP;
                break;
            case 9: case 10: case 11:
                entityType = EntityType.CHICKEN;
                entityTag = Struct.Tag.CHICKEN;
                break;
            case 12:
                entityType = EntityType.HORSE;
                entityTag = Struct.Tag.HORSE;
                break;
            case 13:
                entityType = EntityType.DONKEY;
                entityTag = Struct.Tag.DONKEY;
                break;
            case 14:
                entityType = EntityType.MULE;
                entityTag = Struct.Tag.MULE;
                break;
            case 15: default:
                entityType = EntityType.MUSHROOM_COW;
                entityTag = Struct.Tag.MUSHROOM_COW;
            }
        }
        if (!animalBlocks.isEmpty()) {
            Collections.shuffle(animalBlocks, random);
            int animalCount = (animalBlocks.size() - 1) / 6 + 1;
            int color = randomInt(16);
            for (int i = 0; i < animalCount; i += 1) {
                Block block = animalBlocks.get(i);
                Entity e = block.getWorld().spawnEntity(block.getLocation().add(0.5, 0.0, 0.5), entityType);
                if (e instanceof Colorable) {
                    ((Colorable)e).setColor(DyeColor.values()[color]);
                }
            }
        }
        Cuboid bb = new Cuboid(offset.getX(), offset.getY(), offset.getZ(),
                               offset.getX() + width - 1, offset.getY() + 3, offset.getZ() + height - 1);
        return new Struct(Struct.Type.PASTURE, bb, null, EnumSet.of(entityTag));
    }

    Struct plantLair(Block start, House house, Town town) {
        List<Integer> heights = new ArrayList<>(house.tiles.size());
        for (Vec2 vec: house.tiles.keySet()) {
            heights.add(findHighestBlock(start.getRelative(vec.x, 0, vec.y)).getY());
        }
        int roomHeight = 8;
        Collections.sort(heights);
        int floorLevel = heights.get(heights.size() / 2) - roomHeight - 1;
        Block offset = start.getWorld().getBlockAt(start.getX(), floorLevel, start.getZ());
        {
            int ax, az, bx, bz;
            ax = az = Integer.MAX_VALUE;
            bx = bz = Integer.MIN_VALUE;
            int ay = floorLevel;
            int by = floorLevel + roomHeight;
            for (Room room: house.rooms) {
                Cuboid bb = new Cuboid(room.ax + offset.getX(), ay, room.ay + offset.getZ(),
                                       room.bx + offset.getX(), by, room.by + offset.getZ());
                room.boundingBox = bb;
                if (ax > bb.ax) ax = bb.ax;
                if (az > bb.az) az = bb.az;
                if (bx < bb.bx) bx = bb.bx;
                if (bz < bb.bz) bz = bb.bz;
            }
            house.boundingBox = new Cuboid(ax, ay, az, bx, by, bz);
        }
        for (Vec2 vec: house.tiles.keySet()) {
            Block floor = offset.getRelative(vec.x, 0, vec.y);
            RoomTile tile = house.tiles.get(vec);
            if (random.nextBoolean()) {
                Tile.COBBLESTONE.setBlock(floor);
            } else {
                Tile.MOSSY_COBBLESTONE.setBlock(floor);
            }
            Block ceil = floor.getRelative(0, roomHeight, 0);
            if (ceil.getType().isSolid()) {
                if (random.nextBoolean()) {
                    Tile.COBBLESTONE.setBlock(ceil);
                } else {
                    Tile.MOSSY_COBBLESTONE.setBlock(ceil);
                }
            }
            for (int i = 1; i < roomHeight; i += 1) {
                Block block = floor.getRelative(0, i, 0);
                switch (tile) {
                case DOOR:
                    if (i < 3) {
                        Tile.AIR.setBlock(block);
                        break;
                    }
                    // fallthrough
                case WALL:
                case WINDOW:
                    switch (random.nextInt(10)) {
                    case 0: Tile.MOSSY_STONE_BRICKS.setBlock(block); break;
                    case 1: Tile.CRACKED_STONE_BRICKS.setBlock(block); break;
                    default: Tile.STONE_BRICKS.setBlock(block);
                    }
                    break;
                case FLOOR:
                default:
                    Tile.AIR.setBlock(block);
                }
            }
        }
        List<Struct.Tag> allTags = Arrays.asList(Struct.Tag.ZOMBIE, Struct.Tag.HUSK, Struct.Tag.SKELETON, Struct.Tag.STRAY, Struct.Tag.CREEPER, Struct.Tag.SPIDER, Struct.Tag.CAVE_SPIDER);
        List<Struct.Tag> possibleTags = new ArrayList<>(allTags.size());
        for (Struct.Tag tag: allTags) {
            if (town == null || town.fraction == null || !town.fraction.villagerTypes.contains(tag.entityType)) {
                possibleTags.add(tag);
            }
        }
        Set<Struct.Tag> tags = EnumSet.noneOf(Struct.Tag.class);
        if (!possibleTags.isEmpty()) {
            Struct.Tag tag = possibleTags.get(random.nextInt(possibleTags.size()));
            tags.add(tag);
            for (Room room: house.rooms) {
                int x = random.nextInt(room.width() - 2) + 1;
                int z = random.nextInt(room.height() - 2) + 1;
                Block block = offset.getRelative(room.ax + x, 2, room.ay + z);
                Tile.MOB_SPAWNER.setBlock(block);
                org.bukkit.block.CreatureSpawner state = (org.bukkit.block.CreatureSpawner)block.getState();
                state.setSpawnedType(tag.entityType);
                state.update();
            }
        }
        return new Struct(Struct.Type.LAIR, house.boundingBox,
                          house.rooms.stream().map(r -> new Struct(Struct.Type.LAIR_ROOM, r.boundingBox, null, null)).collect(Collectors.toList()),
                          tags);
    }

    Struct plantMine(Block start, int width, int height) {
        List<Integer> highest = new ArrayList<>();
        Map<Vec2, Integer> tiles = new HashMap<>();
        for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                highest.add(findHighestBlock(start.getRelative(x, 0, y)).getY());
            }
        }
        Collections.sort(highest);
        int floorLevel = highest.get(highest.size() / 2);
        Block offset = start.getWorld().getBlockAt(start.getX(), floorLevel, start.getZ());
        style = new Style(uniqueFlags.get(Flag.Strategy.STYLE), random.nextInt(16));
        for (int z = 0; z < height; z += 1) {
            for (int x = 0; x < width; x += 1) {
                boolean outerX, outerZ, isWall, isCorner;
                outerX = x == 0 || x == width - 1;
                outerZ = z == 0 || z == height - 1;
                isCorner = outerX && outerZ;
                isWall = !isCorner && (outerX || outerZ);
                Orientation ori = outerX ? Orientation.VERTICAL : Orientation.HORIZONTAL;
                Block floor = offset.getRelative(x, 0, z);
                for (int y = 0; y < floorLevel - 10; y += 1) {
                    Block block = floor.getRelative(0, -y, 0);
                    if (isCorner) {
                        if (y == 0) {
                            style.cornerBase.orient(ori).setBlock(block);
                        } else if (y < 4) {
                            style.corner.setBlock(block);
                        }
                    } else if (isWall) {
                        if (y == 0) {
                            style.wallBase.orient(ori).setBlock(block);
                        } else if (y < 4) {
                            style.wall.setBlock(block);
                        }
                    } else {
                        block.setType(Material.AIR);
                    }
                }
                int roomHeight = 5;
                for (int y = 1; y <= roomHeight; y += 1) {
                    Block block = floor.getRelative(0, y, 0);
                    if (isCorner) {
                        if (y < roomHeight) {
                            style.pillar.setBlock(block);
                        } else {
                            style.slab.setBlock(block);
                        }
                    } else if (isWall) {
                        if (y < roomHeight) {
                            Tile.AIR.setBlock(block);
                        } else {
                            style.slab.setBlock(block);
                        }
                    } else {
                        Tile.AIR.setBlock(block);
                    }
                }
            }
        }
        return new Struct(Struct.Type.MINE,
                          new Cuboid(offset.getX(), offset.getY() - 8, offset.getZ(),
                                     offset.getX() + width - 1, offset.getY() + 4, offset.getZ() + height - 1),
                          null, null);
    }

    House generateHouse(int width, int height) {
        Map<Vec2, RoomTile> tiles = new HashMap<>();
        Map<Vec2, Room> roomMap = new HashMap<>();
        Map<Room, Set<Room>> roomConnections = new HashMap<>();
        List<Room> rooms = splitRoom(new Room(0, 0, width - 1, height - 1));
        // Remove rooms
        if (rooms.size() > 2) {
            List<Room> removableRooms = rooms.stream().filter(r -> (r.width() != width && r.height() != height) && (r.ax == 0 || r.ay == 0 || r.bx == width - 1 || r.by == height - 1)).collect(Collectors.toList());
            if (!removableRooms.isEmpty()) {
                Collections.shuffle(removableRooms, random);
                int removeCount = Math.min(rooms.size() - 2, 1 + randomInt(removableRooms.size()));
                for (int i = 0; i < removeCount; i += 1) {
                    Room room = removableRooms.get(i);
                    rooms.remove(room);
                    if (!roomsConnected(rooms)) rooms.add(room);
                }
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
        // Insert doors and windows
        Collections.shuffle(rooms, random);
        int totalOutsideDoors = 1 + randomInt(3);
        double windowChance;
        switch (uniqueFlags.get(Flag.Strategy.PURPOSE)) {
        case SHOP: windowChance = 0.75; break;
        default: windowChance = 0.35;
        }
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
                if (wall == RoomTile.WALL && inside == RoomTile.FLOOR) {
                    if (outside == null) {
                        if (!flags.contains(Flag.UNDERGROUND)) {
                            if (!outsideDoor && totalOutsideDoors > 0 && left == RoomTile.WALL && right == RoomTile.WALL) {
                                tiles.put(vec, RoomTile.DOOR);
                                outsideDoor = true;
                                totalOutsideDoors -= 1;
                            } else if (left != RoomTile.DOOR && right != RoomTile.DOOR) {
                                if (random.nextDouble() < windowChance) {
                                    tiles.put(vec, RoomTile.WINDOW);
                                }
                            }
                        }
                    } else if (outside == RoomTile.FLOOR && left == RoomTile.WALL && right == RoomTile.WALL) {
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
        } else if (width <= 8 && height <= 8 && randomInt(3) > 0) {
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
            int split = 3 + randomInt(width - 6);
            result.addAll(splitRoom(new Room(room.ax, room.ay,
                                             room.ax + split, room.by)));
            result.addAll(splitRoom(new Room(room.ax + split, room.ay,
                                             room.bx, room.by)));
        } else {
            int split = 3 + randomInt(height - 6);
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
                    boolean conX = (room1.ax == room2.bx || room1.bx == room2.ax) && room1.ay < room2.by - 1 && room1.by > room2.ay + 1;
                    boolean conY = (room1.ay == room2.by || room1.by == room2.ay) && room1.ax < room2.bx - 1 && room1.bx > room2.ax + 1;
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
            return bx - ax + 1;
        }

        int height() {
            return by - ay + 1;
        }
    }

    @RequiredArgsConstructor
    static final class House {
        final List<Room> rooms;
        final Map<Vec2, RoomTile> tiles;
        final Map<Vec2, Room> roomMap;
        final List<Vec3> npcs = new ArrayList<>();
        public Vec3 offset;
        public Cuboid boundingBox;
        List<Vec2> decorationList;
    }

    @RequiredArgsConstructor
    static final class Town {
        final int ax, ay, bx, by;
        final List<Vec2> chunks;
        final List<Vec2> wilderChunks;
        final List<House> houses = new ArrayList<>();
        final List<Struct> structs = new ArrayList<>();
        String name;
        Fraction fraction;
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

    enum Flag {
        COBBLE(Strategy.STYLE),
        SANDSTONE(Strategy.STYLE),
        RED_SANDSTONE(Strategy.STYLE),
        STONEBRICK(Strategy.STYLE),
        BRICKS(Strategy.STYLE),
        KOONTZY(Strategy.STYLE),
        OAK(Strategy.STYLE),
        SPRUCE(Strategy.STYLE),
        BIRCH(Strategy.STYLE),
        JUNGLE(Strategy.STYLE),
        ACACIA(Strategy.STYLE),
        DARK_OAK(Strategy.STYLE),
        CONCRETE(Strategy.STYLE),
        TERRACOTTA(Strategy.STYLE),
        STONE(Strategy.STYLE),
        ANDESITE(Strategy.STYLE),
        DIORITE(Strategy.STYLE),
        GRANITE(Strategy.STYLE),
        QUARTZ(Strategy.STYLE),
        PURPUR(Strategy.STYLE),
        NETHER(Strategy.STYLE, true),
        PRISMARINE(Strategy.STYLE),
        IRON(Strategy.STYLE),
        WOOL(Strategy.STYLE),

        NO_ROOF(Strategy.RANDOM),
        NO_BASE(Strategy.RANDOM),
        NO_DECORATION(Strategy.RANDOM),

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

        SHOP(Strategy.PURPOSE),

        RANDOM(Strategy.RANDOM);
        enum Strategy {
            ALTITUDE, STYLE, DOOR, RANDOM, PURPOSE;
        }
        final Strategy strategy;
        final boolean rare;
        Flag(Strategy strategy) {
            this.strategy = strategy;
            this.rare = false;
        }
        Flag(Strategy strategy, boolean rare) {
            this.strategy = strategy;
            this.rare = rare;
        }
    }

    class Style {
        final Tile corner, cornerBase, cornerTop;
        final Tile wall, wallBase, wallTop, wallRandom;
        final Tile floor, ceiling;
        final Tile foundation;
        final Tile roofStair, roofSlab, roofDoubleSlab;
        final Tile pillar, window;
        final Tile stair, slab, fence;
        Tile lamp = Tile.GLOWSTONE;
        Tile torch = Tile.TORCH;
        Tile gate = Tile.OAK_FENCE_GATE;
        final int baseLevel;
        final double randomWallChance;
        Tile floorAlt, ceilingAlt;
        final int color;
        Style(Flag flag, int color) {
            this.color = color;
            switch (flag) {
            case STONEBRICK:
                wall = wallTop = Tile.STONE_BRICKS;
                wallBase = Tile.COBBLESTONE;
                wallRandom = Tile.MOSSY_STONE_BRICKS;
                corner = cornerBase = cornerTop = Tile.OAK_LOG;
                foundation = Tile.STONE_BRICKS;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                floor = ceiling = Tile.OAK_PLANKS;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.STONE_BRICK_SLAB;
                stair = Tile.STONE_BRICK_STAIRS;
                baseLevel = 1;
                randomWallChance = 0.0;
                break;
            case SANDSTONE:
                wall = wallRandom = Tile.SMOOTH_DOUBLE_SANDSTONE_SLAB;
                wallBase = Tile.SANDSTONE;
                wallTop = Tile.SMOOTH_SANDSTONE;
                corner = Tile.SMOOTH_SANDSTONE;
                cornerBase = cornerTop = Tile.CHISELED_SANDSTONE;
                foundation = Tile.SANDSTONE;
                roofStair = Tile.JUNGLE_WOOD_STAIRS;
                roofSlab = Tile.JUNGLE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_JUNGLE_WOOD_SLAB;
                floor = ceiling = Tile.OAK_PLANKS;
                pillar = fence = Tile.SPRUCE_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.SANDSTONE_SLAB;
                stair = Tile.SANDSTONE_STAIRS;
                gate = Tile.SPRUCE_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case RED_SANDSTONE:
                wall = wallRandom = Tile.SMOOTH_DOUBLE_RED_SANDSTONE_SLAB;
                wallBase = Tile.RED_SANDSTONE;
                wallTop = Tile.SMOOTH_RED_SANDSTONE;
                corner = Tile.SMOOTH_RED_SANDSTONE;
                cornerBase = cornerTop = Tile.CHISELED_RED_SANDSTONE;
                foundation = Tile.RED_SANDSTONE;
                roofStair = Tile.ACACIA_WOOD_STAIRS;
                roofSlab = Tile.ACACIA_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_ACACIA_WOOD_SLAB;
                floor = ceiling = Tile.DOUBLE_STONE_SLAB;
                pillar = fence = Tile.ACACIA_FENCE;
                gate = Tile.ACACIA_FENCE_GATE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.RED_SANDSTONE_SLAB;
                stair = Tile.RED_SANDSTONE_STAIRS;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case QUARTZ:
                wall = wallRandom = Tile.QUARTZ_BLOCK;
                wallBase = wallTop = Tile.CHISELED_QUARTZ_BLOCK;
                corner = cornerBase = cornerTop = Tile.PILLAR_QUARTZ_BLOCK;
                foundation = Tile.SMOOTH_DOUBLE_STONE_SLAB;
                roofStair = Tile.QUARTZ_STAIRS;
                roofSlab = Tile.QUARTZ_SLAB;
                roofDoubleSlab = Tile.DOUBLE_QUARTZ_SLAB;
                floor = ceiling = Tile.DOUBLE_STONE_SLAB;
                pillar = fence = Tile.BIRCH_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.QUARTZ_SLAB;
                stair = Tile.QUARTZ_STAIRS;
                gate = Tile.BIRCH_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case BRICKS:
                wall = wallRandom = foundation = Tile.BRICKS;
                wallTop = Tile.SPRUCE_LOG.or(4);
                wallBase = Tile.STONE_BRICKS;
                corner = cornerBase = cornerTop = Tile.SPRUCE_LOG;
                roofStair = Tile.SPRUCE_WOOD_STAIRS;
                roofSlab = Tile.SPRUCE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_SPRUCE_WOOD_SLAB;
                floor = ceiling = Tile.SPRUCE_PLANKS;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.BRICK_SLAB;
                stair = Tile.BRICK_STAIRS;
                gate = Tile.SPRUCE_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case KOONTZY:
                wall = wallBase = foundation = Tile.BRICKS;
                wallRandom = Tile.TERRACOTTA.or(color);
                wallTop = Tile.SPRUCE_LOG.or(4);
                corner = cornerBase = cornerTop = Tile.SPRUCE_LOG;
                roofStair = Tile.SPRUCE_WOOD_STAIRS;
                roofSlab = Tile.SPRUCE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_SPRUCE_WOOD_SLAB;
                floor = ceiling = Tile.SPRUCE_PLANKS;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.BRICK_SLAB;
                stair = Tile.BRICK_STAIRS;
                gate = Tile.SPRUCE_FENCE_GATE;
                baseLevel = 0;
                randomWallChance = 0.5;
                break;
            case OAK:
                wall = wallRandom = foundation = Tile.OAK_PLANKS;
                wallTop = Tile.OAK_LOG.or(4);
                wallBase = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.OAK_LOG;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.COBBLESTONE;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                pillar = fence = Tile.OAK_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.OAK_WOOD_SLAB;
                stair = Tile.OAK_WOOD_STAIRS;
                gate = Tile.OAK_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case SPRUCE:
                wall = wallRandom = foundation = Tile.SPRUCE_PLANKS;
                wallTop = Tile.SPRUCE_LOG.or(4);
                wallBase = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.SPRUCE_LOG;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.COBBLESTONE;
                roofStair = Tile.SPRUCE_WOOD_STAIRS;
                roofSlab = Tile.SPRUCE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_SPRUCE_WOOD_SLAB;
                pillar = fence = Tile.SPRUCE_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.SPRUCE_WOOD_SLAB;
                stair = Tile.SPRUCE_WOOD_STAIRS;
                gate = Tile.SPRUCE_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case BIRCH:
                wall = wallRandom = foundation = Tile.BIRCH_PLANKS;
                wallTop = Tile.BIRCH_LOG.or(4);
                wallBase = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.BIRCH_LOG;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.COBBLESTONE;
                roofStair = Tile.BIRCH_WOOD_STAIRS;
                roofSlab = Tile.BIRCH_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_BIRCH_WOOD_SLAB;
                pillar = fence = Tile.BIRCH_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.BIRCH_WOOD_SLAB;
                stair = Tile.BIRCH_WOOD_STAIRS;
                gate = Tile.BIRCH_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case JUNGLE:
                wall = wallRandom = foundation = Tile.JUNGLE_PLANKS;
                wallTop = Tile.JUNGLE_LOG.or(4);
                wallBase = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.JUNGLE_LOG;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.COBBLESTONE;
                roofStair = Tile.JUNGLE_WOOD_STAIRS;
                roofSlab = Tile.JUNGLE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_JUNGLE_WOOD_SLAB;
                pillar = fence = Tile.JUNGLE_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.JUNGLE_WOOD_SLAB;
                stair = Tile.JUNGLE_WOOD_STAIRS;
                gate = Tile.JUNGLE_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case ACACIA:
                wall = wallRandom = foundation = Tile.ACACIA_PLANKS;
                wallTop = Tile.ACACIA_LOG.or(4);
                wallBase = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.ACACIA_LOG;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.COBBLESTONE;
                roofStair = Tile.ACACIA_WOOD_STAIRS;
                roofSlab = Tile.ACACIA_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_ACACIA_WOOD_SLAB;
                pillar = fence = Tile.ACACIA_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.ACACIA_WOOD_SLAB;
                stair = Tile.ACACIA_WOOD_STAIRS;
                gate = Tile.ACACIA_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case DARK_OAK:
                wall = wallRandom = foundation = Tile.DARK_OAK_PLANKS;
                wallTop = Tile.DARK_OAK_LOG.or(4);
                wallBase = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.DARK_OAK_LOG;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.COBBLESTONE;
                roofStair = Tile.DARK_OAK_WOOD_STAIRS;
                roofSlab = Tile.DARK_OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_DARK_OAK_WOOD_SLAB;
                pillar = fence = Tile.DARK_OAK_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.DARK_OAK_WOOD_SLAB;
                stair = Tile.DARK_OAK_WOOD_STAIRS;
                gate = Tile.DARK_OAK_FENCE_GATE;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case CONCRETE:
                wall = wallRandom = foundation = Tile.CONCRETE.or(color);
                wallBase = wallTop = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.STONE_BRICKS;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.SPRUCE_PLANKS;
                roofStair = Tile.SPRUCE_WOOD_STAIRS;
                roofSlab = Tile.SPRUCE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_SPRUCE_WOOD_SLAB;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.STONE_BRICK_SLAB;
                stair = Tile.STONE_BRICK_STAIRS;
                baseLevel = 0;
                randomWallChance = -1.0;
                break;
            case TERRACOTTA:
                wall = wallRandom = foundation = Tile.TERRACOTTA.or(color);
                wallBase = Tile.STONE_BRICKS;
                wallTop = Tile.SPRUCE_LOG.or(4);
                corner = cornerBase = Tile.STONE_BRICKS;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                ceiling = Tile.SPRUCE_PLANKS;
                int floorOri1 = random.nextInt(4);
                switch (color) {
                case 0: floor = Tile.of(Material.WHITE_GLAZED_TERRACOTTA, floorOri1); break;
                case 1: floor = Tile.of(Material.ORANGE_GLAZED_TERRACOTTA, floorOri1); break;
                case 2: floor = Tile.of(Material.MAGENTA_GLAZED_TERRACOTTA, floorOri1); break;
                case 3: floor = Tile.of(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, floorOri1); break;
                case 4: floor = Tile.of(Material.YELLOW_GLAZED_TERRACOTTA, floorOri1); break;
                case 5: floor = Tile.of(Material.LIME_GLAZED_TERRACOTTA, floorOri1); break;
                case 6: floor = Tile.of(Material.PINK_GLAZED_TERRACOTTA, floorOri1); break;
                case 7: floor = Tile.of(Material.GRAY_GLAZED_TERRACOTTA, floorOri1); break;
                case 8: floor = Tile.of(Material.SILVER_GLAZED_TERRACOTTA, floorOri1); break;
                case 9: floor = Tile.of(Material.CYAN_GLAZED_TERRACOTTA, floorOri1); break;
                case 10: floor = Tile.of(Material.PURPLE_GLAZED_TERRACOTTA, floorOri1); break;
                case 11: floor = Tile.of(Material.BLUE_GLAZED_TERRACOTTA, floorOri1); break;
                case 12: floor = Tile.of(Material.BROWN_GLAZED_TERRACOTTA, floorOri1); break;
                case 13: floor = Tile.of(Material.GREEN_GLAZED_TERRACOTTA, floorOri1); break;
                case 14: floor = Tile.of(Material.RED_GLAZED_TERRACOTTA, floorOri1); break;
                case 15: default: floor = Tile.of(Material.BLACK_GLAZED_TERRACOTTA, floorOri1);
                }
                floorAlt = floor.with((random.nextInt(3) + floorOri1) % 4);
                roofStair = Tile.SPRUCE_WOOD_STAIRS;
                roofSlab = Tile.SPRUCE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_SPRUCE_WOOD_SLAB;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.STONE_BRICK_SLAB;
                stair = Tile.STONE_BRICK_STAIRS;
                baseLevel = 0;
                randomWallChance = -1.0;
                break;
            case STONE:
                wall = Tile.STONE;
                wallRandom = foundation = Tile.COBBLESTONE;
                wallBase = Tile.COBBLESTONE;
                wallTop = Tile.STONE_BRICKS;
                corner = cornerBase = Tile.STONE_BRICKS;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                floor = ceiling = Tile.OAK_PLANKS;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.STONE_BRICK_SLAB;
                stair = Tile.STONE_BRICK_STAIRS;
                baseLevel = 1;
                randomWallChance = 0.0;
                break;
            case GRANITE:
                wall = wallRandom = foundation = Tile.GRANITE;
                wallBase = wallTop = corner = cornerBase = cornerTop = Tile.POLISHED_GRANITE;
                floor = ceiling = Tile.OAK_PLANKS;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.COBBLESTONE_SLAB;
                stair = Tile.COBBLESTONE_STAIRS;
                baseLevel = 0;
                randomWallChance = -1.0;
                break;
            case DIORITE:
                wall = wallRandom = foundation = Tile.DIORITE;
                wallBase = wallTop = corner = cornerBase = cornerTop = Tile.POLISHED_DIORITE;
                floor = ceiling = Tile.OAK_PLANKS;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.COBBLESTONE_SLAB;
                stair = Tile.COBBLESTONE_STAIRS;
                baseLevel = 0;
                randomWallChance = -1.0;
                break;
            case ANDESITE:
                wall = wallRandom = foundation = Tile.ANDESITE;
                wallBase = wallTop = corner = cornerBase = cornerTop = Tile.POLISHED_ANDESITE;
                floor = ceiling = Tile.OAK_PLANKS;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.COBBLESTONE_SLAB;
                stair = Tile.COBBLESTONE_STAIRS;
                baseLevel = 0;
                randomWallChance = -1.0;
                break;
            case PURPUR:
                wall = wallRandom = foundation = Tile.END_STONE_BRICKS;
                wallBase = Tile.PURPUR_BLOCK;
                wallTop = Tile.PURPUR_PILLAR.or(4);
                corner = Tile.PURPUR_PILLAR;
                cornerBase = cornerTop = Tile.PURPUR_BLOCK;
                floor = ceiling = Tile.PURPUR_BLOCK;
                floorAlt = ceilingAlt = Tile.PURPUR_PILLAR;
                roofStair = Tile.PURPUR_STAIRS;
                roofSlab = Tile.PURPUR_SLAB;
                roofDoubleSlab = Tile.PURPUR_DOUBLE_SLAB;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.PURPUR_SLAB;
                stair = Tile.PURPUR_STAIRS;
                torch = Tile.END_ROD;
                baseLevel = 0;
                randomWallChance = -1.0;
                break;
            case NETHER:
                wall = wallRandom = foundation = Tile.NETHER_BRICK;
                floor = Tile.of(Material.MAGMA);
                ceiling = Tile.of(Material.NETHER_WART_BLOCK);
                corner = cornerBase = cornerTop = wallBase = wallTop = Tile.RED_NETHER_BRICK;
                roofStair = Tile.NETHER_BRICK_STAIRS;
                roofSlab = Tile.NETHER_BRICK_SLAB;
                roofDoubleSlab = Tile.DOUBLE_NETHER_BRICK_SLAB;
                pillar = fence = window = Tile.NETHER_BRICK_FENCE;
                slab = Tile.NETHER_BRICK_SLAB;
                stair = Tile.NETHER_BRICK_STAIRS;
                gate = Tile.DARK_OAK_FENCE_GATE;
                torch = Tile.of(Material.REDSTONE_TORCH_ON);
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case PRISMARINE:
                wall = foundation = Tile.PRISMARINE;
                wallRandom = Tile.MOSSY_COBBLESTONE;
                floor = ceiling = Tile.DARK_PRISMARINE;
                corner = Tile.ACACIA_LOG;
                cornerBase = Tile.MOSSY_STONE_BRICKS;
                cornerTop = Tile.SEA_LANTERN;
                wallBase = wallTop = Tile.PRISMARINE_BRICKS;
                roofStair = Tile.SPRUCE_WOOD_STAIRS;
                roofSlab = Tile.SPRUCE_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_SPRUCE_WOOD_SLAB;
                window = Tile.STAINED_GLASS_PANE.or(color);
                pillar = fence = Tile.COBBLESTONE_WALL;
                slab = Tile.SPRUCE_WOOD_SLAB;
                stair = Tile.SPRUCE_WOOD_STAIRS;
                gate = Tile.OAK_FENCE_GATE;
                lamp = Tile.SEA_LANTERN;
                baseLevel = 1;
                randomWallChance = 0.25;
                break;
            case IRON:
                wall = foundation = Tile.STONE_BRICKS;
                wallRandom = Tile.CRACKED_STONE_BRICKS;
                floor = ceiling = Tile.COBBLESTONE;
                cornerBase = cornerTop = wallBase = wallTop = Tile.IRON_BLOCK;
                corner = Tile.COBBLESTONE;
                window = pillar = fence = Tile.IRON_BARS;
                slab = roofSlab = Tile.COBBLESTONE_SLAB;
                stair = roofStair = Tile.COBBLESTONE_STAIRS;
                roofDoubleSlab = Tile.DOUBLE_COBBLESTONE_SLAB;
                gate = Tile.OAK_FENCE_GATE;
                torch = Tile.of(Material.REDSTONE_TORCH_ON);
                baseLevel = 1;
                randomWallChance = 0.25;
                break;
            case WOOL:
                wall = wallRandom = foundation = Tile.WOOL.or(color);
                wallBase = Tile.COBBLESTONE;
                wallTop = Tile.OAK_LOG.or(4);
                floor = ceiling = Tile.OAK_PLANKS;
                corner = cornerBase = Tile.OAK_LOG;
                cornerTop = Tile.CHISELED_STONE_BRICKS;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                pillar = fence = Tile.OAK_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.OAK_WOOD_SLAB;
                stair = Tile.OAK_WOOD_STAIRS;
                baseLevel = 1;
                randomWallChance = -1.0;
                break;
            case COBBLE:
            default:
                wall = Tile.COBBLESTONE;
                wallBase = Tile.OAK_LOG.or(4);
                wallTop = Tile.OAK_LOG.or(4);
                wallRandom = Tile.MOSSY_COBBLESTONE;
                corner = Tile.OAK_LOG;
                cornerBase = cornerTop = Tile.CHISELED_STONE_BRICKS;
                foundation = Tile.COBBLESTONE;
                roofStair = Tile.OAK_WOOD_STAIRS;
                roofSlab = Tile.OAK_WOOD_SLAB;
                roofDoubleSlab = Tile.DOUBLE_OAK_WOOD_SLAB;
                floor = ceiling = Tile.OAK_PLANKS;
                pillar = fence = Tile.COBBLESTONE_WALL;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.COBBLESTONE_SLAB;
                stair = Tile.COBBLESTONE_STAIRS;
                baseLevel = 0;
                randomWallChance = 0.0;
            }
            if (floorAlt == null) floorAlt = floor;
            if (ceilingAlt == null) ceilingAlt = ceiling;
        }
    }

    enum Decoration {
        TORCH,
        BOOKSHELF,
        CAULDRON,
        BREWING_STAND,
        JUKEBOX,
        WORKBENCH,
        FURNACE,
        CHEST,
        CHAIR,
        FLOWER_POT,
        ANVIL,
        BANNER,
        TABLE,
        ENCHANTMENT_TABLE,
        ENDER_CHEST,
        LADDER,
        BED;
    }
}
