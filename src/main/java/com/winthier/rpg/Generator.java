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
import org.bukkit.material.MaterialData;

final class Generator {
    final RPGPlugin plugin;
    final Random random = new Random(System.currentTimeMillis());
    final Set<Material> replaceMats = EnumSet.of(Material.LOG, Material.LOG_2, Material.LEAVES, Material.LEAVES_2, Material.PUMPKIN, Material.HUGE_MUSHROOM_1, Material.HUGE_MUSHROOM_2);
    final Map<Vec2, Block> highestBlocks = new HashMap<>();
    private Set<Flag> flags = EnumSet.noneOf(Flag.class);
    private Map<Flag.Strategy, Flag> uniqueFlags = new EnumMap<>(Flag.Strategy.class);
    private Town town;

    void setFlags(Collection<Flag> newFlags) {
        this.flags.clear();
        this.flags.addAll(newFlags);
        uniqueFlags.clear();
        for (Flag.Strategy strat: Flag.Strategy.values()) uniqueFlags.put(strat, Flag.RANDOM);
        for (Flag flag: flags) {
            uniqueFlags.put(flag.strategy, flag);
        }
    }

    Generator(RPGPlugin plugin) {
        this.plugin = plugin;
    }

    Generator() {
        this.plugin = null;
    }

    String generateName() {
        return generateName(1 + random.nextInt(3));
    }

    String generateName(int syllables) {
        final String[] beginSyllable = {"b", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "qu", "r", "s", "t", "v", "w", "x", "z", "sh", "st", "sn", "sk", "sl", "sm", "ch", "kr", "fr", "gr", "tr", "y", "bl", "ph", "pl", "pr", "str", "chr", "schw", "th", "thr", "thn"};
        final String[] vocals = {"a", "e", "i", "o", "u"};
        final String[] longVocals = {"aa", "ee", "oo"};
        final String[] diphtongs = {"au", "ei", "ou"};
        final String[] accents = {"á", "à", "é", "è", "ó", "ò", "ú", "ù"};
        final String[] umlauts = {"ä", "ö", "ü"};
        final String[] endSyllable = {"b", "d", "f", "g", "k", "l", "m", "n", "p", "r", "s", "t", "v", "w", "x", "z", "st", "nd", "sd", "sh", "tsh", "sch", "ng", "nk", "rk", "lt", "ld", "rn", "rt", "rs", "ts", "mb", "rst", "tch", "ch"};
        final String[] endStrong = {"ff", "gg", "kk", "ll", "mm", "nn", "pp", "rr", "ss", "tt", "tz", "ck", "th", "rth", "nth", "ns"};
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
                return generateName(syllables);
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
        double size = world.getWorldBorder().getSize() * 0.5;
        Chunk chunkMin = world.getWorldBorder().getCenter().add(-size, 0, -size).getBlock().getRelative(-1, 0, -1).getChunk();
        Chunk chunkMax = world.getWorldBorder().getCenter().add(size, 0, size).getBlock().getRelative(1, 0, 1).getChunk();
        List<Chunk> todo = new ArrayList<>();
        todo.add(centerChunk);
        Set<Chunk> done = new HashSet<>();
        List<Vec2> chunks = new ArrayList<>();
        List<Integer> allHeights = new ArrayList<>();
        while (!todo.isEmpty() && chunks.size() < sizeInChunks) {
            Chunk chunk = todo.remove(0);
            if (done.contains(chunk)) continue;
            done.add(chunk);
            if (chunk.getX() <= chunkMin.getX() || chunk.getX() >= chunkMax.getX() ||
                chunk.getZ() <= chunkMin.getZ() || chunk.getZ() >= chunkMax.getZ()) continue;
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
                Collections.shuffle(todo, random);
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
        this.town = new Town(ax * 16, ay * 16, bx * 16 + 15, by * 16 + 15, chunks);
        return this.town;
    }

    void plantTown(World world, Town town) {
        Collections.shuffle(town.chunks);
        int fountains = 1;
        int farms = 1 + random.nextInt(3);
        List<Block> roadBlocks = new ArrayList<>();
        for (Vec2 chunk: town.chunks) {
            if (town.chunks.contains(chunk.relative(1, 0))) {
                for (int z = 0; z < 16; z += 1) {
                    roadBlocks.add(findHighestBlock(world.getBlockAt(chunk.x * 16 + 15, 0, chunk.y * 16 + z)));
                }
            }
            if (town.chunks.contains(chunk.relative(-1, 0))) {
                for (int z = 0; z < 16; z += 1) {
                    roadBlocks.add(findHighestBlock(world.getBlockAt(chunk.x * 16, 0, chunk.y * 16 + z)));
                }
            }
            if (town.chunks.contains(chunk.relative(0, 1))) {
                for (int x = 0; x < 16; x += 1) {
                    roadBlocks.add(findHighestBlock(world.getBlockAt(chunk.x * 16 + x, 0, chunk.y * 16 + 15)));
                }
            }
            if (town.chunks.contains(chunk.relative(0, -1))) {
                for (int x = 0; x < 16; x += 1) {
                    roadBlocks.add(findHighestBlock(world.getBlockAt(chunk.x * 16 + x, 0, chunk.y * 16)));
                }
            }
        }
        for (Block block: roadBlocks) {
            Block upper = block.getRelative(0, 1, 0);
            while (upper.getType() != Material.AIR) {
                upper.setType(Material.AIR);
                upper = upper.getRelative(0, 1, 0);
            }
            Tile.of(Material.DIRT).setBlockNoPhysics(block.getRelative(0, -1, 0));
            Tile.of(Material.GRASS_PATH).setBlockNoPhysics(block);
        }
        for (Vec2 chunk: town.chunks) {
            Chunk bchunk = world.getChunkAt(chunk.x, chunk.y);
            for (Entity e: bchunk.getEntities()) {
                if (e instanceof LivingEntity && ((LivingEntity)e).getRemoveWhenFarAway()) e.remove();
            }
            if (fountains > 0) {
                fountains -= 1;
                int size = 3 + random.nextInt(4);
                int offx = 1 + random.nextInt(13 - size);
                int offy = 1 + random.nextInt(13 - size);
                plantFountain(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), size);
            } else if (farms > 0) {
                farms -= 1;
                int width = 7 + random.nextInt(3) * 2;
                int height = 7 + random.nextInt(3) * 2;
                int offx = 1 + random.nextInt(13 - width);
                int offy = 1 + random.nextInt(13 - height);
                plantFarm(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), width, height);
            } else {
                int width = 4 + random.nextInt(11);
                int height = 4 + random.nextInt(11);
                int offx = width >= 13 ? 1 : 1 + random.nextInt(13 - width);
                int offy = height >= 13 ? 1 : 1 + random.nextInt(13 - height);
                House house = generateHouse(width, height);
                town.houses.add(house);
                house.townId = town.townId;
                plantHouse(world.getBlockAt(chunk.x * 16 + offx, 0, chunk.y * 16 + offy), house);
            }
        }
    }

    void plantMonument(Block center) {
        Block block = findHighestBlock(center);
    }

    void plantHouse(Block start, House house) {
        Flag flagAltitude = uniqueFlags.get(Flag.Strategy.ALTITUDE);
        Map<Vec2, RoomTile> tiles = house.tiles;
        boolean noRoof = flags.contains(Flag.NO_ROOF) || flagAltitude == Flag.UNDERGROUND;
        boolean noBase = flags.contains(Flag.NO_BASE) || flagAltitude == Flag.FLOATING;
        boolean noDecoration = flags.contains(Flag.NO_DECORATION);
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
        Block offset = start.getWorld().getBlockAt(start.getX(), floorLevel, start.getZ());
        house.offset = new Vec3(offset.getX(), offset.getY(), offset.getZ());
        {
            int ax, az, bx, bz;
            ax = az = Integer.MAX_VALUE;
            bx = bz = Integer.MIN_VALUE;
            int ay = floorLevel;
            int by = floorLevel + 4;
            house.boundingBox = new Cuboid(ax, ay, az, bx, by, bz);
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
        Material matDoor;
        Flag flagDoor = uniqueFlags.get(Flag.Strategy.DOOR);
        Flag flagStyle = uniqueFlags.get(Flag.Strategy.STYLE);
        switch (flagDoor) {
        case RANDOM:
            switch (flagStyle) {
            case OAK: matDoor = Material.WOODEN_DOOR; break;
            case SPRUCE: matDoor = Material.SPRUCE_DOOR; break;
            case BIRCH: matDoor = Material.BIRCH_DOOR; break;
            case JUNGLE: matDoor = Material.JUNGLE_DOOR; break;
            case ACACIA: matDoor = Material.ACACIA_DOOR; break;
            case DARK_OAK: matDoor = Material.DARK_OAK_DOOR; break;
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
        int roomHeight = 4 + random.nextInt(3);
        int windowHeight = 1 + random.nextInt(roomHeight - 3);
        Style style = new Style(flagStyle, color);
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
            if (flagAltitude != Flag.UNDERGROUND && flagAltitude != Flag.HERE) {
                Block upper = floor.getRelative(0, 1, 0);
                while (upper.getType() != Material.AIR) {
                    upper.setType(Material.AIR);
                    upper = upper.getRelative(0, 1, 0);
                }
            }
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
            int beds = 1 + random.nextInt(house.rooms.size());
            for (Room room: house.rooms) {
                boolean bed = false;
                int amount = room.width() + room.height();
                int torches = 2 + random.nextInt(3);
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
                List<Vec2> decl = new ArrayList<>(decs);
                Collections.shuffle(decl, random);
                for (Vec2 vec: decl) {
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
                    Block floor = offset.getRelative(vec.x, 0, vec.y);
                    house.tiles.put(vec, RoomTile.DECORATION);
                    if (!bed && beds > 0 && decl.contains(vec.relative(facing.rotate().vector)) && tiles.get(vec.relative(facing.rotate().vector)) == RoomTile.FLOOR) {
                        bed = true;
                        beds -= 1;
                        Vec2 nbor = vec.relative(facing.rotate().vector);
                        tiles.put(nbor, RoomTile.DECORATION);
                        Facing faceDown = facing.rotate();
                        Facing faceUp = faceDown.opposite();
                        Block blockHead = floor.getRelative(0, 1, 0);
                        Block blockBottom = floor.getRelative(faceDown.vector.x, 1, faceDown.vector.y);
                        Tile.of(Material.BED_BLOCK).or(faceUp.dataBed | 8).setBlockNoPhysics(blockHead);
                        Tile.of(Material.BED_BLOCK).or(faceUp.dataBed).setBlockNoPhysics(blockBottom);
                        DyeColor bedColor = DyeColor.values()[color];
                        org.bukkit.block.Bed bedState;
                        bedState = (org.bukkit.block.Bed)blockHead.getState();
                        bedState.setColor(bedColor);
                        bedState.update();
                        bedState = (org.bukkit.block.Bed)blockBottom.getState();
                        bedState.setColor(bedColor);
                        bedState.update();
                    } else if (torches > 0) {
                        torches -= 1;
                        floor.getRelative(0, window ? 1 : 2, 0).setTypeIdAndData(Material.TORCH.getId(), (byte)facing.dataTorch, true);
                    } else {
                        switch (random.nextInt(24)) {
                        case 0:
                            if (!window) floor.getRelative(0, 3, 0).setType(Material.BOOKSHELF);
                            // fallthrough
                        case 1:
                            if (!window) floor.getRelative(0, 2, 0).setType(Material.BOOKSHELF);
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
                            floor.getRelative(0, window ? 1 : 2, 0).setTypeIdAndData(Material.REDSTONE_TORCH_ON.getId(), (byte)facing.dataTorch, true);
                            break;
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                            style.stair.or(facing.dataStair | 4).setBlock(floor.getRelative(0, 1, 0));
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
                            banner.setBaseColor(DyeColor.values()[color]);
                            int patternCount = 1 + random.nextInt(4);
                            List<Pattern> patterns = new ArrayList<>(patternCount);
                            for (int j = 0; j < patternCount; j += 1) {
                                patterns.add(new Pattern(DyeColor.values()[random.nextInt(DyeColor.values().length)], PatternType.values()[random.nextInt(PatternType.values().length)]));
                            }
                            banner.setPatterns(patterns);
                            banner.update();
                            break;
                        case 19:
                            floor.getRelative(0, 1, 0).setType(Material.FENCE);
                            floor.getRelative(0, 2, 0).setType(Material.WOOD_PLATE);
                            break;
                        case 20:
                            Tile.of(Material.ENCHANTMENT_TABLE).setBlock(floor.getRelative(0, 1, 0));
                            break;
                        case 21:
                            Tile.of(Material.ENDER_CHEST, facing.dataBlock).setBlock(floor.getRelative(0, 1, 0));
                            break;
                        case 22:
                            if (!window) {
                                for (int i = 2; i < roomHeight; i += 1) {
                                    Tile.of(Material.LADDER, facing.dataBlock).setBlock(floor.getRelative(0, i, 0));
                                }
                            }
                            Tile.of(Material.LADDER, facing.dataBlock).setBlock(floor.getRelative(0, 1, 0));
                            break;
                        case 23:
                        default:
                            Vec2 nbor = vec.relative(facing.rotate().vector);
                            if (decl.contains(nbor) && tiles.get(nbor) == RoomTile.FLOOR) {
                                tiles.put(nbor, RoomTile.DECORATION);
                                Facing faceDown = facing.rotate();
                                Facing faceUp = faceDown.opposite();
                                Block blockHead = floor.getRelative(0, 1, 0);
                                Block blockBottom = floor.getRelative(faceDown.vector.x, 1, faceDown.vector.y);
                                Tile.of(Material.BED_BLOCK).or(faceUp.dataBed | 8).setBlockNoPhysics(blockHead);
                                Tile.of(Material.BED_BLOCK).or(faceUp.dataBed).setBlockNoPhysics(blockBottom);
                                DyeColor bedColor = DyeColor.values()[color];
                                org.bukkit.block.Bed bedState;
                                bedState = (org.bukkit.block.Bed)blockHead.getState();
                                bedState.setColor(bedColor);
                                bedState.update();
                                bedState = (org.bukkit.block.Bed)blockBottom.getState();
                                bedState.setColor(bedColor);
                                bedState.update();
                            } else {
                                style.stair.or(facing.dataStair).setBlock(floor.getRelative(0, 1, 0));
                            }
                            break;
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
        for (int i = 0; i < house.rooms.size() - 1; i += 1) totalNPCs += random.nextInt(2);
        List<Vec2> possibleNPCSpots = new ArrayList<>();
        for (Room room: house.rooms) {
            List<Vec2> vecs = new ArrayList<>();
            for (int x = room.ax + 1; x < room.bx; x += 1) {
                for (int y = room.ay + 1; y < room.by; y += 1) {
                    Vec2 vec = new Vec2(x, y);
                    if (house.tiles.get(vec) == RoomTile.FLOOR) vecs.add(vec);
                }
            }
            if (!vecs.isEmpty()) possibleNPCSpots.add(vecs.get(random.nextInt(vecs.size())));
        }
        Collections.shuffle(possibleNPCSpots, random);
        totalNPCs = Math.min(possibleNPCSpots.size(), totalNPCs);
        for (int i = 0; i < totalNPCs; i += 1) {
            Vec2 vec = possibleNPCSpots.get(i);
            house.tiles.put(vec, RoomTile.NPC);
            house.npcs.add(house.offset.relative(vec.x, 1, vec.y));
        }
    }

    void plantFountain(Block start, int size) {
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
        Style style = new Style(uniqueFlags.get(Flag.Strategy.STYLE), 0);
        int height = 4 + random.nextInt(3);
        boolean nether = uniqueFlags.get(Flag.Strategy.STYLE) == Flag.NETHER;
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
                        if (i < 2) {
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
        switch (random.nextInt(9)) {
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
        if (town != null) {
            Cuboid bb = new Cuboid(offset.getX(), offset.getY() - 16, offset.getZ(),
                                   offset.getX() + size, offset.getY() + height, offset.getZ() + size);
            town.structures.add(new Structure("fountain", bb, Arrays.asList(bb)));
        }
    }

    void plantFarm(Block start, int width, int height) {
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
        Style style = new Style(uniqueFlags.get(Flag.Strategy.STYLE), 0);
        boolean nether = uniqueFlags.get(Flag.Strategy.STYLE) == Flag.NETHER;
        Tile fruit, soil;
        if (nether) {
            fruit = Tile.of(Material.NETHER_WARTS, 3);
            soil = Tile.of(Material.SOUL_SAND);
        } else {
            switch (random.nextInt(5)) {
            case 0: fruit = Tile.of(Material.BEETROOT_BLOCK, 3); break;
            case 1: fruit = Tile.of(Material.CARROT, 7); break;
            case 2: fruit = Tile.of(Material.POTATO, 7); break;
            case 3: default: fruit = Tile.of(Material.CROPS, 7);
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
                    tileAbove = Tile.of(Material.GLOWSTONE);
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
                Block upper = block.getRelative(0, 1, 0);
                while (upper.getType() != Material.AIR) {
                    Tile.AIR.setBlock(upper);
                    upper = upper.getRelative(0, 1, 0);
                }
                tile.setBlockNoPhysics(block);
                tileAbove.setBlockNoPhysics(block.getRelative(0, 1, 0));
                if (isCenter) {
                    style.slab.setBlock(block.getRelative(0, 2, 0));
                }
            }
        }
        if (town != null) {
            Cuboid bb = new Cuboid(offset.getX(), offset.getY(), offset.getZ(),
                                   offset.getX() + width, offset.getY() + 1, offset.getZ() + height);
            town.structures.add(new Structure("farm", bb, Arrays.asList(bb)));
        }
    }

    House generateHouse(int width, int height) {
        Map<Vec2, RoomTile> tiles = new HashMap<>();
        Map<Vec2, Room> roomMap = new HashMap<>();
        Map<Room, Set<Room>> roomConnections = new HashMap<>();
        List<Room> rooms = splitRoom(new Room(0, 0, width - 1, height - 1));
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
                if (wall == RoomTile.WALL && inside == RoomTile.FLOOR) {
                    if (outside == null) {
                        if (!flags.contains(Flag.UNDERGROUND)) {
                            if (!outsideDoor && totalOutsideDoors > 0 && left == RoomTile.WALL && right == RoomTile.WALL) {
                                tiles.put(vec, RoomTile.DOOR);
                                outsideDoor = true;
                                totalOutsideDoors -= 1;
                            } else if (left != RoomTile.DOOR && right != RoomTile.DOOR) {
                                if (random.nextBoolean()) {
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
        public Vec3 offset;
        public Cuboid boundingBox;
        int townId;
    }

    @RequiredArgsConstructor
    static final class Town {
        final int ax, ay, bx, by;
        final List<Vec2> chunks;
        final List<House> houses = new ArrayList<>();
        final List<Structure> structures = new ArrayList<>();
        int townId;
        String name;
    }

    @RequiredArgsConstructor
    static final class Structure {
        final String name;
        final Cuboid boundingBox;
        final List<Cuboid> boundingBoxes;
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

        RANDOM(Strategy.RANDOM);
        enum Strategy {
            ALTITUDE, STYLE, DOOR, RANDOM;
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
        final int baseLevel;
        final double randomWallChance;
        Tile floorAlt, ceilingAlt;
        Style(Flag flag, int color) {
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
                pillar = fence = Tile.BIRCH_FENCE;
                window = Tile.of(Material.STAINED_GLASS_PANE, color);
                slab = Tile.SANDSTONE_SLAB;
                stair = Tile.SANDSTONE_STAIRS;
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
}
