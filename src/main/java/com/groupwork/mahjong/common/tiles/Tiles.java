package com.groupwork.mahjong.common.tiles;

import java.util.ArrayList;
import java.util.Collections;

public class Tiles {
    public static final Tile WILDCARD = new Tile(TileType.UNKNOWN, (byte) 0);
    private static final ArrayList<Tile> holder = new ArrayList<>(35);

    public static void init() {
        holder.add(WILDCARD);
        for (var kind : TileType.values()) {
            for (int i = 1; i <= kind.maxKind; i++) holder.add(new Tile(kind, (byte) i));
        }
    }

    public static Tile getTile(byte id) {
        return holder.get(id);
    }

    public static TileGroup getShuffledTiles() {
        TileGroup result = new TileGroup(false);
        for (var tile : Tiles.holder) {
            if (tile == Tiles.WILDCARD) continue;
            for (int i = 0; i <= 3; i++) result.add(tile);
        }
        result.trimToSize();
        Collections.shuffle(result);
        return result;
    }
}
