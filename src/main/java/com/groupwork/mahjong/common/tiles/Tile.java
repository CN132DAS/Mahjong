package com.groupwork.mahjong.common.tiles;

public record Tile(TileType tileType, byte number) implements Comparable<Tile> {
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
}
