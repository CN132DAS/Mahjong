package com.groupwork.mahjong.common.tiles;

public enum TileType {
    CHARACTER(9), // 万
    BAMBOO(9), // 条
    CIRCLE(9), // 筒
    WIND(4), // 风
    DRAGON(3), // 三元
    UNKNOWN(0);
    public final int maxKind;

    TileType(int maxKind) {
        this.maxKind = maxKind;
    }
}
