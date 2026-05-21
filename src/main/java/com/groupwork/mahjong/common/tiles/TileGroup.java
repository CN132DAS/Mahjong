package com.groupwork.mahjong.common.tiles;

import java.util.ArrayList;
import java.util.Collections;

public class TileGroup extends ArrayList<Tile> {
    private final boolean keepSorted;

    public TileGroup(boolean keepSorted) {
        super();
        this.keepSorted = keepSorted;
    }

    @Override
    public boolean add(Tile tile) {
        boolean result = super.add(tile);
        if (keepSorted) Collections.sort(this);
        return result;
    }

    public boolean remove(Tile tile) {
        boolean result = super.remove(tile);
        if (!result) result = super.remove(Tiles.WILDCARD);
        if (keepSorted) Collections.sort(this);
        return result;
    }

    public Tile[] drawTiles(int num) {
        Tile[] result = new Tile[num];
        for (int i = 0; i < num; i++) {
            result[i] = this.getFirst();
            this.removeFirst();
        }
        return result;
    }
}
