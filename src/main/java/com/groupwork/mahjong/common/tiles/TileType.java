package com.groupwork.mahjong.common.tiles;

public enum TileType {
    CHARACTER(9, "万"), // 万
    BAMBOO(9, "条"), // 条
    CIRCLE(9, "饼"), // 筒
    WIND(4, "风"), // 风
    DRAGON(3, ""), // 三元
    UNKNOWN(0, "");
    public final int maxKind;
    public final String name;

    TileType(int maxKind, String name) {
        this.maxKind = maxKind;
        this.name = name;
    }
}
