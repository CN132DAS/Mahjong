package com.groupwork.mahjong.common.tiles;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record Tile(TileType tileType, byte number) implements Comparable<Tile> {
    private static final String[] description1 =
            new String[] {"一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] description2 = new String[] {"东", "南", "西", "北"};
    private static final String[] description3 = new String[] {"红中", "白板", "发财"};

    @Override
    public int compareTo(Tile o) {
        if (o == Tiles.WILDCARD) return -1;
        int kind = this.tileType.ordinal() - o.tileType.ordinal();
        return kind == 0 ? this.number - o.number : kind;
    }

    public byte getID() {
        return (byte)
                (switch (tileType) {
                            case TileType.UNKNOWN -> 0;
                            case TileType.WIND -> 27;
                            case TileType.DRAGON -> 31;
                            default -> tileType.ordinal() * 9;
                        }
                        + number);
    }

    public boolean hasChiCombinations() {
        return tileType.ordinal() <= 2;
    }

    public Tile[] getChiCombination() {
        byte id = getID();
        Tile after1 = Tiles.getTile((byte) (id + 1));
        Tile after2 = Tiles.getTile((byte) (id + 2));
        return new Tile[] {this, after1, after2};
    }

    public List<Tile[]> getAllChiCombinations() {
        byte id = getID();
        Tile before2 = Tiles.getTile((byte) (id - 2));
        Tile before1 = Tiles.getTile((byte) (id - 1));
        Tile after1 = Tiles.getTile((byte) (id + 1));
        Tile after2 = Tiles.getTile((byte) (id + 2));
        if (number == 1) return List.<Tile[]>of(new Tile[] {this, after1, after2});
        else if (number == 2)
            return List.of(new Tile[] {before1, this, after1}, new Tile[] {this, after1, after2});
        else if (number == 8)
            return List.of(new Tile[] {before2, before1, this}, new Tile[] {before1, this, after1});
        else if (number == 9) return List.<Tile[]>of(new Tile[] {before2, before1, this});
        else
            return List.of(
                    new Tile[] {before2, before1, this},
                    new Tile[] {before1, this, after1},
                    new Tile[] {this, after1, after2});
    }

    public String getFullDescription() {
        return getShortDescription() + tileType.name;
    }

    public String getShortDescription() {
        return switch (tileType) {
            case CHARACTER, CIRCLE, BAMBOO -> description1[number - 1];
            case WIND -> description2[number - 1];
            case DRAGON -> description3[number - 1];
            case UNKNOWN -> "未知";
        };
    }

    public boolean same(Tile tile) {
        return tile == this;
    }

    public static boolean isYao(Tile tile) {
        return tile.tileType.ordinal() > 2 || tile.number == 1 || tile.number == 9;
    }

    @Override
    public @NotNull String toString() {
        return number + tileType.name();
    }
}
