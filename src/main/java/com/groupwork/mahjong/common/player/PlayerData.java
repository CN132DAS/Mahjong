package com.groupwork.mahjong.common.player;

import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import java.util.ArrayList;

public class PlayerData {
    private boolean isAdmin = false;
    private PlayerType type = PlayerType.EMPTY;

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

    public PlayerType getType() {
        return type;
    }

    public void setType(PlayerType type) {
        this.type = type;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public static class ServerSpec {
        public ArrayList<TileGroup> tileShown;
        public TileGroup tileInHand;
        public TileGroup tileDiscarded;

        public ServerSpec(
                ArrayList<TileGroup> tileShown, TileGroup tileInHand, TileGroup tileDiscarded) {
            this.tileShown = tileShown;
            this.tileInHand = tileInHand;
            this.tileDiscarded = tileDiscarded;
        }

        public static ServerSpec defaultInstance() {
            return new ServerSpec(new ArrayList<>(), new TileGroup(true), new TileGroup(false));
        }
    }

    public static class ClientSpec {
        public ArrayList<TileGroup> tileShown;
        public TileGroup tileInHand;
        public Tile tileDraw = null;
        public TileGroup tileDiscarded;

        public ClientSpec(
                ArrayList<TileGroup> tileShown, TileGroup tileInHand, TileGroup tileDiscarded) {
            this.tileShown = tileShown;
            this.tileInHand = tileInHand;
            this.tileDiscarded = tileDiscarded;
        }

        public static ClientSpec defaultInstance() {
            return new ClientSpec(new ArrayList<>(), new TileGroup(true), new TileGroup(false));
        }
    }
}
