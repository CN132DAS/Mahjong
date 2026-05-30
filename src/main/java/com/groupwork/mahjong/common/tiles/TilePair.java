package com.groupwork.mahjong.common.tiles;

import org.jetbrains.annotations.NotNull;

public record TilePair(Tile tile, PairType pairType, boolean isFuLu) {
    @Override
    public @NotNull String toString() {
        return tile.toString() + pairType.toString();
    }
}
