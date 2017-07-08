package com.winthier.rpg;

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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.material.MaterialData;

final class Generator {
    final Random random = new Random(System.currentTimeMillis());
    final Set<Material> replaceMats = EnumSet.of(Material.LOG, Material.LOG_2, Material.LEAVES, Material.LEAVES_2);

    void plantHouse(Block start, House house, Set<GeneratorFlag> flags) {
        Map<Vec2, RoomTile> tiles = house.tiles;
        GeneratorFlag flagStyle = GeneratorFlag.RANDOM;
        GeneratorFlag flagDoor = GeneratorFlag.RANDOM;
        GeneratorFlag flagAltitude = GeneratorFlag.SURFACE;
        for (GeneratorFlag flag: flags) {
            if (flag.strategy == GeneratorFlag.Strategy.STYLE) flagStyle = flag;
            if (flag.strategy == GeneratorFlag.Strategy.DOOR) flagDoor = flag;
            if (flag.strategy == GeneratorFlag.Strategy.ALTITUDE) flagAltitude = flag;
        }
        boolean noRoof = flags.contains(GeneratorFlag.NO_ROOF) || flagAltitude == GeneratorFlag.UNDERGROUND;
        boolean noBase = flags.contains(GeneratorFlag.NO_BASE) || flagAltitude != GeneratorFlag.SURFACE;
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
                Block highest = start.getWorld().getBlockAt(start.getX() + vec.x, 0, start.getZ() + vec.y);
                while (highest.getLightFromSky() == 0) highest = highest.getRelative(0, 1, 0);
                highest = highest.getRelative(0, -1, 0);
                Block lower = null;
                do {
                    lower = highest.getRelative(0, -1, 0);
                    if (replaceMats.contains(lower.getType())) {
                        highest = lower;
                    } else {
                        lower = null;
                    }
                } while (lower != null);
                floorLevels.add(highest.getY());
            }
            Collections.sort(floorLevels);
            floorLevel = floorLevels.get(floorLevels.size() / 2);
        }
        Material matDoor;
        switch (flagDoor) {
        case RANDOM:
            switch (random.nextInt(7)) {
            case 0:
                matDoor = Material.ACACIA_DOOR;
                break;
            case 1:
                matDoor = Material.BIRCH_DOOR;
                break;
            case 2:
                matDoor = Material.DARK_OAK_DOOR;
                break;
            case 3:
                matDoor = Material.IRON_DOOR;
                break;
            case 4:
                matDoor = Material.JUNGLE_DOOR;
                break;
            case 5:
                matDoor = Material.SPRUCE_DOOR;
                break;
            case 6:
            default:
                matDoor = Material.WOODEN_DOOR;
            }
            break;
        case ACACIA_DOOR:
            matDoor = Material.ACACIA_DOOR;
            break;
        case BIRCH_DOOR:
            matDoor = Material.BIRCH_DOOR;
            break;
        case DARK_OAK_DOOR:
            matDoor = Material.DARK_OAK_DOOR;
            break;
        case IRON_DOOR:
            matDoor = Material.IRON_DOOR;
            break;
        case JUNGLE_DOOR:
            matDoor = Material.JUNGLE_DOOR;
            break;
        case SPRUCE_DOOR:
            matDoor = Material.SPRUCE_DOOR;
            break;
        case OAK_DOOR:
        default:
            matDoor = Material.WOODEN_DOOR;
        }
        int color = random.nextInt(16);
        for (Vec2 vec: tiles.keySet()) {
            Block floor = start.getWorld().getBlockAt(start.getX() + vec.x, floorLevel, start.getZ() + vec.y);
            Block[] blocks = {
                floor, floor.getRelative(0, 1, 0), floor.getRelative(0, 2, 0), floor.getRelative(0, 3, 0), floor.getRelative(0, 4, 0)
            };
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
                matCeil = matFloor = Material.SANDSTONE;
                mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.SANDSTONE;
                data[1] = 2;
                data[4] = 1;
                matBase = Material.SAND;
                break;
            case QUARTZ:
                matCeil = matFloor = Material.DOUBLE_STEP;
                mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.QUARTZ_BLOCK;
                matBase = Material.DOUBLE_STEP;
                RoomTile ta = tiles.get(vec.relative(1, 0));
                RoomTile tb = tiles.get(vec.relative(0, 1));
                RoomTile tc = tiles.get(vec.relative(-1, 0));
                RoomTile td = tiles.get(vec.relative(0, -1));
                boolean a = ta != null && ta != RoomTile.FLOOR;
                boolean b = tb != null && tb != RoomTile.FLOOR;
                boolean c = tc != null && tc != RoomTile.FLOOR;
                boolean d = td != null && td != RoomTile.FLOOR;
                if ((a && b) || (b && c) || (c && d) || (d && a)) {
                    dataBase = data[0] = data[1] = data[2] = data[3] = data[4] = 2;
                } else {
                    data[1] = 1;
                    data[4] = 1;
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
                if (random.nextBoolean()) {
                    matBase = Material.STAINED_CLAY;
                    dataBase = color;
                } else {
                    matBase = Material.BRICK;
                }
                for (int i = 0; i < mats.length; i += 1) {
                    if (random.nextBoolean()) {
                        mats[i] = Material.STAINED_CLAY;
                        data[i] = color;
                    } else {
                        mats[i] = Material.BRICK;
                    }
                }
                break;
            case WOOD:
                ta = tiles.get(vec.relative(1, 0));
                tb = tiles.get(vec.relative(0, 1));
                tc = tiles.get(vec.relative(-1, 0));
                td = tiles.get(vec.relative(0, -1));
                a = ta != null && ta != RoomTile.FLOOR;
                b = tb != null && tb != RoomTile.FLOOR;
                c = tc != null && tc != RoomTile.FLOOR;
                d = td != null && td != RoomTile.FLOOR;
                if ((a && b) || (b && c) || (c && d) || (d && a)) {
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.LOG;
                } else {
                    if (a || c) {
                        data[4] = 4;
                    } else {
                        data[4] = 8;
                    }
                    matBase = mats[0] = mats[1] = mats[2] = mats[3] = Material.WOOD;
                }
                mats[4] = Material.LOG;
                matFloor = matCeil = Material.COBBLESTONE;
                break;
            case CONCRETE:
                matBase = mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.CONCRETE;
                dataBase = data[0] = data[1] = data[2] = data[3] = data[4] = color;
                matFloor = matCeil = Material.WOOD;
                dataFloor = dataCeil = 1;
                break;
            case TERRACOTTA:
                matBase = mats[0] = mats[1] = mats[2] = mats[3] = mats[4] = Material.STAINED_CLAY;
                dataBase = data[0] = data[1] = data[2] = data[3] = data[4] = color;
                matFloor = matCeil = Material.WOOD;
                dataFloor = dataCeil = 1;
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
            switch (tiles.get(vec)) {
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
                        blocks[i].setTypeIdAndData(matDoor.getId(), (byte)dataDoor, true);
                        break;
                    case 2:
                        blocks[i].setTypeIdAndData(matDoor.getId(), (byte)0x8, true);
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
        // Decorate
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
        // Make a roof
        if (!noRoof) {
            Material doubleSlabMat = Material.WOOD_DOUBLE_STEP;
            int doubleSlabData = 0;
            Material slabMat = Material.WOOD_STEP;
            int slabData = 0;
            switch (flagStyle) {
            case BRICKS:
                doubleSlabData = slabData = 1;
                break;
            case KOONTZY:
                doubleSlabData = slabData = 1;
                break;
            case SANDSTONE:
                doubleSlabMat = Material.DOUBLE_STEP;
                doubleSlabData = 1;
                slabMat = Material.STEP;
                slabData = 1;
                break;
            case TERRACOTTA:
                doubleSlabMat = Material.DOUBLE_STEP;
                doubleSlabData = 4;
                slabMat = Material.STEP;
                slabData = 4;
                break;
            case QUARTZ:
                doubleSlabMat = Material.DOUBLE_STEP;
                doubleSlabData = 0;
                slabMat = Material.STEP;
                slabData = 0;
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
            if (random.nextBoolean()) {
                relx = 1; rely = 0;
            } else {
                relx = 0; rely = 1;
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
            for (Vec2 vec: roofs.keySet()) {
                int roofLevel = roofs.get(vec);
                Block roof1 = start.getWorld().getBlockAt(start.getX() + vec.x, floorLevel + 5, start.getZ() + vec.y);
                Block roof2 = roof1.getRelative(0, roofLevel / 2, 0);
                if (tiles.get(vec) != null) {
                    while (roof1.getY() < roof2.getY()) {
                        if (!roof1.getType().isSolid() || replaceMats.contains(roof1.getType())) {
                            roof1.setTypeIdAndData(doubleSlabMat.getId(), (byte)doubleSlabData, true);
                        }
                        roof1 = roof1.getRelative(0, 1, 0);
                    }
                }
                if (!roof2.getType().isSolid() || replaceMats.contains(roof2.getType())) {
                    if ((roofLevel & 1) == 0) {
                        roof2.setTypeIdAndData(slabMat.getId(), (byte)slabData, true);
                    } else {
                        roof2.setTypeIdAndData(doubleSlabMat.getId(), (byte)doubleSlabData, true);
                    }
                }
            }
        }
    }

    House generateHouse(int width, int height) {
        Map<Vec2, RoomTile> tiles = new HashMap<>();
        Map<Vec2, Room> roomMap = new HashMap<>();
        Map<Room, Set<Room>> roomConnections = new HashMap<>();
        List<Room> rooms = splitRoom(new Room(0, 0, width, height));
        // Remove some rooms
        if (rooms.size() > 2) {
            int remove = random.nextInt(rooms.size() - 2);
            for (int i = 0; i < remove; i += 1) {
                Collections.shuffle(rooms, random);
                Room room = rooms.remove(rooms.size() - 1);
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
                        if (!outsideDoor) {
                            tiles.put(vec, RoomTile.DOOR);
                            outsideDoor = true;
                        } else {
                            if (random.nextBoolean()) {
                                tiles.put(vec, RoomTile.WINDOW);
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
}

@Value
final class Room {
    public final int ax, ay, bx, by;

    int width() {
        return Math.abs(ax - bx) + 1;
    }

    int height() {
        return Math.abs(ay - by) + 1;
    }
}

@Value
final class House {
    public final List<Room> rooms;
    public final Map<Vec2, RoomTile> tiles;
    public final Map<Vec2, Room> roomMap;
}

@Value
final class Vec2 {
    public final int x, y;
    Vec2 relative(int x, int y) {
        return new Vec2(this.x + x, this.y + y);
    }
}

enum RoomTile {
    WALL("|"), FLOOR("."), DOOR("+"), WINDOW("o"), DECORATION("@");
    public final String stringIcon;
    RoomTile(String icon) {
        this.stringIcon = icon;
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

enum GeneratorFlag {
    COBBLE(Strategy.STYLE),
    SANDSTONE(Strategy.STYLE),
    STONEBRICK(Strategy.STYLE),
    BRICKS(Strategy.STYLE),
    KOONTZY(Strategy.STYLE),
    WOOD(Strategy.STYLE),
    CONCRETE(Strategy.STYLE),
    TERRACOTTA(Strategy.STYLE),
    QUARTZ(Strategy.STYLE),
    NO_ROOF(Strategy.RANDOM),
    NO_BASE(Strategy.RANDOM),
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
    GeneratorFlag(Strategy strategy) {
        this.strategy = strategy;
    }
}
