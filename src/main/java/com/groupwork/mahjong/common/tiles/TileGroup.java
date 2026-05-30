package com.groupwork.mahjong.common.tiles;

import com.groupwork.mahjong.common.SortedArrayList;
import java.util.Collection;

public class TileGroup extends SortedArrayList<Tile> {
    private final boolean isAnGang;

    public TileGroup(boolean isAnGang) {
        super();
        this.isAnGang = isAnGang;
    }

    public TileGroup(Collection<Tile> tiles, boolean isAnGang) {
        super(tiles);
        this.isAnGang = isAnGang;
    }

    public boolean remove(Tile tile) {
        boolean result = super.remove(tile);
        if (!result) result = super.removeFirst() != null;
        return result;
    }

    public Tile[] drawTiles(int num, boolean reverse) {
        Tile[] result = new Tile[num];
        for (int i = 0; i < num; i++) {
            if (!reverse) result[i] = this.removeFirst();
            else result[i] = this.removeLast();
        }
        return result;
    }

    public boolean isAnGang() {
        return isAnGang;
    }

    public TilePair toTilePair() {
        if (this.size() > 3) return new TilePair(this.getFirst(), PairType.GANG, isAnGang);
        else {
            Tile tile1 = this.getFirst();
            Tile tile2 = this.getLast();
            return new TilePair(
                    this.getFirst(), tile1 == tile2 ? PairType.PENG : PairType.CHI, true);
        }
    }
}
