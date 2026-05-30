package com.groupwork.mahjong.common.player;

import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import java.util.Random;

public class FakePlayerUtil {
    private static final Random randomSource = new Random();

    public static Tile calculateTileToDiscard(TileGroup group) {
        return group.get(randomSource.nextInt(0, group.size()));
    }
}
