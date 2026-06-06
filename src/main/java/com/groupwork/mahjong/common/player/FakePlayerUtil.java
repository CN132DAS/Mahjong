package com.groupwork.mahjong.common.player;

import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import com.groupwork.mahjong.common.util.HuUtil;
import com.groupwork.mahjong.common.util.ShantenUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FakePlayerUtil {
    private static final Random randomSource = new Random();

    public static boolean canHuWithDiscard(PlayerData data, Tile discardedTile) {
        data.tileInHand.add(discardedTile);
        boolean result = HuUtil.canHu(data);
        data.tileInHand.remove(discardedTile);
        return result;
    }

    public static Tile calculateTileToDiscard(PlayerData data) {
        int bestShanten = Integer.MAX_VALUE;
        List<Tile> bestTiles = new ArrayList<>();
        List<Tile> candidates = new ArrayList<>();
        for (Tile tile : data.tileInHand) {
            if (!candidates.contains(tile)) candidates.add(tile);
        }
        for (Tile tile : candidates) {
            data.tileInHand.remove(tile);
            int shanten = ShantenUtil.calculateNormalShanten(data);
            data.tileInHand.add(tile);
            if (shanten < bestShanten) {
                bestShanten = shanten;
                bestTiles.clear();
                bestTiles.add(tile);
            } else if (shanten == bestShanten && !bestTiles.contains(tile)) {
                bestTiles.add(tile);
            }
        }
        return bestTiles.get(randomSource.nextInt(0, bestTiles.size()));
    }

    public static Tile calculateTileToDiscard(TileGroup group) {
        return group.get(randomSource.nextInt(0, group.size()));
    }
}
