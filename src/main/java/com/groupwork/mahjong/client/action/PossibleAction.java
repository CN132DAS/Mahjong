package com.groupwork.mahjong.client.action;

import com.groupwork.mahjong.common.player.PlayerData;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import com.groupwork.mahjong.common.util.HuUtil;
import java.util.ArrayList;
import java.util.List;

public record PossibleAction(List<IAction> actions) {

    public static PossibleAction beforeDiscard(PlayerData.ClientSpec data, byte spareNum) {
        List<IAction> actions = new ArrayList<>();
        TileGroup obj = new TileGroup(false);
        if (spareNum > 0) {
            obj.addAll(getAnGangCombinations(data.tileInHand));
            obj.addAll(getJiaGangCombinations(data.tileInHand, data.tileShown));
        }
        if (!obj.isEmpty()) actions.add(IAction.Gang(obj.toArray(new Tile[0])));
        if (canZiMo(data)) actions.add(IAction.HU);
        if (!actions.isEmpty()) return new PossibleAction(actions);
        else return null;
    }

    public static PossibleAction afterDiscard(
            PlayerData.ClientSpec data, Tile discardedTile, boolean canChi, byte spareNum) {
        List<IAction> actions = new ArrayList<>();
        if (spareNum > 0 && canNormalGang(data.tileInHand, discardedTile))
            actions.add(IAction.Gang(new Tile[] {discardedTile}));
        if (canPeng(data.tileInHand, discardedTile))
            actions.add(IAction.Peng(new Tile[] {discardedTile}));
        TileGroup obj = new TileGroup(false);
        if (canChi && discardedTile.hasChiCombinations())
            obj.addAll(getChiCombinations(data.tileInHand, discardedTile));
        if (!obj.isEmpty()) actions.add(IAction.Chi(obj.toArray(new Tile[0])));
        if (canHu(data, discardedTile)) {
            actions.add(IAction.HU);
        }
        if (!actions.isEmpty()) {
            actions.add(IAction.SKIP);
            return new PossibleAction(actions);
        } else return null;
    }

    public static PossibleAction afterGangDraw(PlayerData.ClientSpec data) {
        if (false) return new PossibleAction(List.of(IAction.HU));
        return null;
    }

    private static boolean canZiMo(PlayerData data) {
        return HuUtil.canHu(data);
    }

    private static boolean canHu(PlayerData data, Tile discardedTile) {
        data.tileInHand.add(discardedTile);
        boolean result = HuUtil.canHu(data);
        data.tileInHand.remove(discardedTile);
        return result;
    }

    private static List<Tile> getAnGangCombinations(List<Tile> tileInHand) {
        List<Tile> combinations = new ArrayList<>();
        Tile earlierTile = null;
        for (var tile : tileInHand) {
            if (tile == earlierTile) continue;
            if (tileInHand.stream().filter(tile::same).count() == 4) combinations.add(tile);
            earlierTile = tile;
        }
        return combinations;
    }

    private static List<Tile> getJiaGangCombinations(
            List<Tile> tileInHand, List<TileGroup> shownTiles) {
        List<Tile> combinations = new ArrayList<>();
        for (var tiles : shownTiles) {
            var tile = tiles.getFirst();
            if (tiles.stream().filter(tile::same).count() == 3 && tileInHand.contains(tile))
                combinations.add(tile);
        }
        return combinations;
    }

    private static boolean canNormalGang(List<Tile> tileInHand, Tile discardedTile) {
        return tileInHand.stream().filter(discardedTile::same).count() == 3;
    }

    private static boolean canPeng(List<Tile> tileInHand, Tile discardedTile) {
        return tileInHand.stream().filter(discardedTile::same).count() == 2;
    }

    private static List<Tile> getChiCombinations(List<Tile> tileInHand, Tile discardedTile) {
        List<Tile> valid = new ArrayList<>();
        var allCombination = discardedTile.getAllChiCombinations();
        for (var combination : allCombination) {
            boolean flag = true;
            for (var tile : combination) {
                if (tile != discardedTile && !tileInHand.contains(tile)) {
                    flag = false;
                    break;
                }
            }
            if (flag) valid.add(combination[0]);
        }
        return valid;
    }
}
