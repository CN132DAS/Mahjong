package com.groupwork.mahjong.common.player;

import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import java.util.ArrayList;

public class PlayerData {
    private boolean isAdmin = false;
    private PlayerType type = PlayerType.EMPTY;
    public ArrayList<TileGroup> tileShown = new ArrayList<>();
    public TileGroup tileInHand = new TileGroup(false);
    public ArrayList<Tile> tileDiscarded = new ArrayList<>();

    public void clearTileData() {
        tileShown.clear();
        tileInHand.clear();
        tileDiscarded.clear();
    }

    public String getDescriptionString(byte id) {
        if (type == PlayerType.EMPTY) return "空闲";
        else if (type == PlayerType.FAKE) {
            return "电脑玩家" + id;
        } else {
            String result = "玩家" + id;
            if (isAdmin) result += " (房主)";
            if (type == PlayerType.LOCAL) result += " (你)";
            return result;
        }
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public PlayerType getType() {
        return type;
    }

    public void setType(PlayerType type) {
        this.type = type;
    }

    public void swap(PlayerData other) {
        boolean tmpAdmin = this.isAdmin;
        this.isAdmin = other.isAdmin;
        other.isAdmin = tmpAdmin;
        PlayerType tmpType = this.type;
        this.type = other.type;
        other.type = tmpType;
    }

    public static class ClientSpec extends PlayerData {
        public Tile tileDraw = null;

        @Override
        public void clearTileData() {
            super.clearTileData();
            this.tileDraw = null;
        }
    }
}
