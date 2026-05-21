package com.groupwork.mahjong.common.player;

import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;

public class FakePlayerUtil {
    public static Tile calculateTileToDiscard(TileGroup group) {
        return group.getFirst();
    }
}
