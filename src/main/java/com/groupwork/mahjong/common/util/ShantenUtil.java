package com.groupwork.mahjong.common.util;

import com.groupwork.mahjong.common.player.PlayerData;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import com.groupwork.mahjong.common.tiles.TileType;
import com.groupwork.mahjong.common.tiles.Tiles;

public class ShantenUtil {
    private static final int TILE_KIND_COUNT = 35;

    private int bestShanten;

    /*
     * Calculates normal-hand shanten for the common "4 melds + 1 pair" shape.
     *
     * Algorithm idea:
     * - Convert the concealed hand to tile counts.
     * - Recursively try to remove completed melds: triplets and sequences.
     * - Also try incomplete groups (taatsu): pairs, two-sided/edge waits, and closed waits.
     * - At each leaf, use the standard normal-hand estimate:
     *   shanten = 8 - 2 * melds - taatsu - pair
     *   with melds + taatsu capped at 4 groups.
     *
     * Current limitations:
     * - Only normal hands are considered; seven pairs and thirteen orphans are ignored.
     * - Discarded tiles and visible tiles from other players are ignored, so impossible waits can
     *   still be counted as useful by higher-level strategy.
     * - This returns a hand-shape score, not a full Mahjong strategy. It should be combined with
     *   tie breakers such as effective tile count or defensive safety later.
     */
    public static int calculateNormalShanten(PlayerData data) {
        return calculateNormalShanten(data.tileInHand, data.tileShown.size());
    }

    public static int calculateNormalShanten(TileGroup concealedTiles, int openMelds) {
        ShantenUtil util = new ShantenUtil();
        int[] counts = toCounts(concealedTiles);
        util.bestShanten = 8;
        util.search(counts, openMelds, 0, false);
        return util.bestShanten;
    }

    private static int[] toCounts(TileGroup tiles) {
        int[] counts = new int[TILE_KIND_COUNT];
        for (Tile tile : tiles) {
            int id = tile.getID();
            if (id > 0) counts[id]++;
        }
        return counts;
    }

    private void search(int[] counts, int melds, int taatsu, boolean hasPair) {
        int index = firstNonEmpty(counts);
        if (index == -1) {
            updateBest(melds, taatsu, hasPair);
            return;
        }

        if (counts[index] >= 3) {
            counts[index] -= 3;
            search(counts, melds + 1, taatsu, hasPair);
            counts[index] += 3;
        }

        if (canStartSequence(index) && counts[index + 1] > 0 && counts[index + 2] > 0) {
            counts[index]--;
            counts[index + 1]--;
            counts[index + 2]--;
            search(counts, melds + 1, taatsu, hasPair);
            counts[index]++;
            counts[index + 1]++;
            counts[index + 2]++;
        }

        if (counts[index] >= 2) {
            counts[index] -= 2;
            if (!hasPair) search(counts, melds, taatsu, true);
            search(counts, melds, taatsu + 1, hasPair);
            counts[index] += 2;
        }

        if (canStartConsecutiveTaatsu(index) && counts[index + 1] > 0) {
            counts[index]--;
            counts[index + 1]--;
            search(counts, melds, taatsu + 1, hasPair);
            counts[index]++;
            counts[index + 1]++;
        }

        if (canStartSequence(index) && counts[index + 2] > 0) {
            counts[index]--;
            counts[index + 2]--;
            search(counts, melds, taatsu + 1, hasPair);
            counts[index]++;
            counts[index + 2]++;
        }

        counts[index]--;
        search(counts, melds, taatsu, hasPair);
        counts[index]++;
    }

    private static int firstNonEmpty(int[] counts) {
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > 0) return i;
        }
        return -1;
    }

    private void updateBest(int melds, int taatsu, boolean hasPair) {
        if (melds > 4) melds = 4;
        if (melds + taatsu > 4) taatsu = 4 - melds;
        int shanten = 8 - 2 * melds - taatsu - (hasPair ? 1 : 0);
        bestShanten = Math.min(bestShanten, shanten);
    }

    private static boolean canStartSequence(int tileId) {
        Tile tile = tileOf(tileId);
        return isNumberTile(tile) && tile.number() <= 7;
    }

    private static boolean canStartConsecutiveTaatsu(int tileId) {
        Tile tile = tileOf(tileId);
        return isNumberTile(tile) && tile.number() <= 8;
    }

    private static boolean isNumberTile(Tile tile) {
        return tile.tileType() == TileType.CHARACTER
                || tile.tileType() == TileType.BAMBOO
                || tile.tileType() == TileType.CIRCLE;
    }

    private static Tile tileOf(int tileId) {
        return Tiles.getTile((byte) tileId);
    }
}
