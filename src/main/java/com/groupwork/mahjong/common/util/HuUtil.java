package com.groupwork.mahjong.common.util;

import com.groupwork.mahjong.common.SortedArrayList;
import com.groupwork.mahjong.common.player.PlayerData;
import com.groupwork.mahjong.common.tiles.PairType;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TilePair;
import com.groupwork.mahjong.common.tiles.Tiles;
import java.util.ArrayList;
import java.util.List;

public class HuUtil {
    public static boolean canHu(PlayerData playerData) {
        return !getAllHuCombinations(playerData).isEmpty();
    }

    public static List<TilePair[]> getAllHuCombinations(PlayerData playerData) {
        int pairsNeeded = 5 - playerData.tileShown.size();
        TilePair[] shown = new TilePair[playerData.tileShown.size()];
        for (int i = 0; i < playerData.tileShown.size(); i++)
            shown[i] = playerData.tileShown.get(i).toTilePair();
        SortedArrayList<Tile> tiles = new SortedArrayList<>(playerData.tileInHand);
        List<TilePair[]> result =
                new ArrayList<>(getNormalHuCombinations(pairsNeeded, tiles, false));
        for (var pairs : result) {
            for (int i = 0; i < playerData.tileShown.size(); i++) pairs[i] = shown[i];
        }
        TilePair[] qiDui = getQiDuiHu(playerData.tileInHand);
        if (qiDui != null) result.add(qiDui);
        TilePair[] shiSanYao = getShiSanYao(playerData.tileInHand);
        if (shiSanYao != null) result.add(shiSanYao);
        return result;
    }

    public static List<TilePair[]> getNormalHuCombinations(
            int pairsNeeded, SortedArrayList<Tile> tiles, boolean hasPairs) {
        ArrayList<TilePair[]> result = new ArrayList<>();
        int place = 5 - pairsNeeded;
        Tile tile = tiles.getFirst();
        if (tiles.stream().filter(tile::same).count() > 2) {
            var pengCombination = List.of(tile, tile, tile);
            for (var t : pengCombination) tiles.remove(t);
            if (tiles.isEmpty() || pairsNeeded == 1) {
                TilePair[] group = new TilePair[5];
                group[place] = new TilePair(tile, PairType.PENG, false);
                tiles.addAll(pengCombination);
                return new ArrayList<>(List.<TilePair[]>of(group));
            } else {
                var list = getNormalHuCombinations(pairsNeeded - 1, tiles, hasPairs);
                if (!list.isEmpty()) {
                    for (var combination : list)
                        combination[place] = new TilePair(tile, PairType.PENG, false);
                    result.addAll(list);
                }
            }
            tiles.addAll(pengCombination);
        }
        if (tile.hasChiCombinations()) {
            var chiCombination = List.of(tile.getChiCombination());
            if (tiles.containsAll(chiCombination)) {
                for (var t : chiCombination) tiles.remove(t);
                if (tiles.isEmpty() || pairsNeeded == 1) {
                    TilePair[] group = new TilePair[5];
                    group[place] = new TilePair(tile, PairType.CHI, false);
                    tiles.addAll(chiCombination);
                    return new ArrayList<>(List.<TilePair[]>of(group));
                } else {
                    var list = getNormalHuCombinations(pairsNeeded - 1, tiles, hasPairs);
                    if (!list.isEmpty()) {
                        for (var combination : list)
                            combination[place] = new TilePair(tile, PairType.CHI, false);
                        result.addAll(list);
                    }
                }
                tiles.addAll(chiCombination);
            }
        }
        if (!hasPairs && tiles.stream().filter(tile::same).count() == 2) {
            var pair = List.of(tile, tile);
            for (var t : pair) tiles.remove(t);
            if (tiles.isEmpty() || pairsNeeded == 1) {
                TilePair[] group = new TilePair[5];
                group[place] = new TilePair(tile, PairType.DUI, false);
                tiles.addAll(pair);
                return new ArrayList<>(List.<TilePair[]>of(group));
            } else {
                var list = getNormalHuCombinations(pairsNeeded - 1, tiles, true);
                if (!list.isEmpty()) {
                    for (var combination : list)
                        combination[place] = new TilePair(tile, PairType.DUI, false);
                    result.addAll(list);
                }
            }
            tiles.addAll(pair);
        }
        return result;
    }

    public static TilePair[] getQiDuiHu(SortedArrayList<Tile> tiles) {
        if (tiles.size() != 14) return null;
        TilePair[] result = new TilePair[7];
        int num = 0;
        int count = 0;
        Tile prev = null;
        for (var tile : tiles) {
            if (tile == prev) count++;
            else {
                if (prev == null) prev = tile;
                else {
                    if (count != 2) return null;
                    else {
                        result[num] = new TilePair(tile, PairType.DUI, false);
                        num++;
                    }
                }
                count = 1;
            }
        }
        return result;
    }

    private static TilePair[] getShiSanYao(SortedArrayList<Tile> tiles) {
        if (tiles.size() == 14
                && tiles.stream().allMatch(Tile::isYao)
                && tiles.stream().distinct().count() == 13)
            return new TilePair[] {new TilePair(Tiles.WILDCARD, PairType.SHI_SAN_YAO, false)};
        return null;
    }
}
