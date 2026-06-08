package com.groupwork.mahjong.common;

import com.groupwork.mahjong.Mahjong;
import com.groupwork.mahjong.client.action.PossibleAction;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.player.PlayerData;
import com.groupwork.mahjong.common.player.PlayerType;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import com.groupwork.mahjong.common.tiles.Tiles;
import java.util.List;
import java.util.Random;

public abstract class GameData {
    protected final PlayerData[] playerData = new PlayerData[4];
    protected byte currentPlayerId;

    public void setCurrentPlayerId(byte currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public byte getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void changePos(byte id1, byte id2) {
        playerData[id1].swap(playerData[id2]);
    }

    public void handleDiscard(Tile tile) {
        playerData[currentPlayerId].tileInHand.remove(tile);
        playerData[currentPlayerId].tileDiscarded.add(tile);
    }

    public void handleChi(byte playerId, Tile tile) {
        Tile[] combination = tile.getChiCombination();
        Tile discarded = playerData[currentPlayerId].tileDiscarded.removeLast();
        for (var aTile : combination)
            if (aTile != discarded) playerData[playerId].tileInHand.remove(aTile);
        playerData[playerId].tileShown.add(new TileGroup(List.of(combination), false));
        currentPlayerId = playerId;
    }

    public void handlePeng(byte playerId) {
        Tile tile = playerData[currentPlayerId].tileDiscarded.removeLast();
        for (int i = 0; i < 2; i++) playerData[playerId].tileInHand.remove(tile);
        playerData[playerId].tileShown.add(new TileGroup(List.of(tile, tile, tile), false));
        currentPlayerId = playerId;
    }

    public void handleNormalGang(byte playerId) {
        Tile tile = playerData[currentPlayerId].tileDiscarded.removeLast();
        for (int i = 0; i < 3; i++) playerData[playerId].tileInHand.remove(tile);
        playerData[playerId].tileShown.add(new TileGroup(List.of(tile, tile, tile, tile), false));
        currentPlayerId = playerId;
    }

    public void handleAnGang(Tile tile) {
        var data = playerData[currentPlayerId];
        data.tileInHand.removeAll(List.of(tile));
        data.tileShown.add(new TileGroup(List.of(tile, tile, tile, tile), true));
    }

    public boolean handleJiaGang(Tile tile) {
        var data = playerData[currentPlayerId];
        for (var tiles : data.tileShown)
            if (tiles.stream().filter(tile::same).count() == 3) {
                tiles.add(tile);
                data.tileInHand.remove(tile);
                return true;
            }
        return false;
    }

    public static class Server extends GameData {
        private TileGroup spareTiles;

        {
            for (int i = 0; i <= 3; i++) playerData[i] = new PlayerData();
        }

        public void init() {
            for (int i = 0; i <= 3; i++) playerData[i].clearTileData();
            spareTiles = Tiles.getShuffledTiles();
            currentPlayerId = (byte) new Random().nextInt(0, 4);
            if (playerData[currentPlayerId].getType() == PlayerType.EMPTY) nextPlayer();
        }

        public void destruct() {
            for (int i = 0; i <= 3; i++) playerData[i].clearTileData();
            spareTiles = null;
        }

        public PlayerData[] getPlayerData() {
            return playerData;
        }

        public TileGroup getSpareTiles() {
            return spareTiles;
        }

        public void nextPlayer() {
            do {
                currentPlayerId = (byte) ((currentPlayerId + 1) % 4);
            } while (playerData[currentPlayerId].getType() == PlayerType.EMPTY);
        }
    }

    public static class Client extends GameData {
        private final PlayerData.ClientSpec[] playerData = new PlayerData.ClientSpec[4];
        private PossibleAction possibleAction;
        private boolean start;
        private boolean beforeDiscard;
        private byte tickCount;
        private byte localId = 0;
        private byte spareNum;

        {
            for (int i = 0; i <= 3; i++) {
                playerData[i] = new PlayerData.ClientSpec();
                super.playerData[i] = playerData[i];
            }
        }

        @Override
        public void changePos(byte id1, byte id2) {
            super.changePos(id1, id2);
            if (id1 == localId) localId = id2;
        }

        @Override
        public void handleDiscard(Tile tile) {
            super.handleDiscard(tile);
            playerData[currentPlayerId].tileDraw = null;
        }

        @Override
        public void handleAnGang(Tile tile) {
            super.handleAnGang(tile);
            Mahjong.getInstance().refresh();
        }

        @Override
        public boolean handleJiaGang(Tile tile) {
            if (super.handleJiaGang(tile)) {
                Mahjong.getInstance().refresh();
                return true;
            }
            return false;
        }

        public void init() {
            for (int i = 0; i <= 3; i++) playerData[i].clearTileData();
            currentPlayerId = (byte) -1;
            start = false;
            beforeDiscard = true;
        }

        public void destruct() {
            for (int i = 0; i <= 3; i++) playerData[i].clearTileData();
        }

        public boolean canChi() {
            byte id = currentPlayerId;
            do id = (byte) ((id + 1) % 4);
            while (playerData[id].getType() == PlayerType.EMPTY);
            return id == localId;
        }

        public PlayerData.ClientSpec[] getPlayerData() {
            return playerData;
        }

        public String getPlayerDescription(byte id) {
            return playerData[id].getDescriptionString(id);
        }

        public void bind(Messages.ServerInfo message) {
            playerData[message.adminId()].setAdmin(true);
            for (int i = 0; i <= 3; i++) playerData[i].setType(message.types()[i]);
            localId = message.receiveId();
            playerData[localId].setType(PlayerType.LOCAL);
        }

        public PlayerData.ClientSpec getLocalPlayerData() {
            return playerData[localId];
        }

        public PossibleAction getPossibleAction() {
            return possibleAction;
        }

        public void setPossibleAction(PossibleAction possibleAction) {
            this.possibleAction = possibleAction;
        }

        public boolean isStart() {
            return start;
        }

        public void setStart(boolean start) {
            this.start = start;
        }

        public boolean isYourTurn() {
            return localId == currentPlayerId;
        }

        public boolean isBeforeDiscard() {
            return beforeDiscard;
        }

        public void setBeforeDiscard(boolean beforeDiscard) {
            this.beforeDiscard = beforeDiscard;
        }

        public void setTickCount(byte tickCount) {
            this.tickCount = tickCount;
        }

        public byte getTickCount() {
            return tickCount;
        }

        public byte getLocalId() {
            return localId;
        }

        public void setSpareNum(byte spareNum) {
            this.spareNum = spareNum;
        }

        public byte getSpareNum() {
            return spareNum;
        }

        public boolean isAdmin() {
            return playerData[localId].isAdmin();
        }
    }
}
