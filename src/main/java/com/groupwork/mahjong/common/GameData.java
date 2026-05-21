package com.groupwork.mahjong.common;

import com.groupwork.mahjong.common.player.PlayerData;
import com.groupwork.mahjong.common.player.PlayerType;
import com.groupwork.mahjong.common.tiles.TileGroup;
import com.groupwork.mahjong.common.tiles.Tiles;
import java.util.Random;

public class GameData {
    protected PlayerData[] playerData = new PlayerData[4];

    public GameData() {
        for (int i = 0; i <= 3; i++) playerData[i] = new PlayerData();
    }

    public void changePos(byte id1, byte id2) {
        PlayerData util = playerData[id1];
        playerData[id1] = playerData[id2];
        playerData[id2] = util;
    }

    public PlayerData[] getPlayerData() {
        return playerData;
    }

    public void setPlayerData(PlayerData[] playerData) {
        this.playerData = playerData;
    }

    public static class Server extends GameData {
        private final PlayerData.ServerSpec[] playerTiles = new PlayerData.ServerSpec[4];
        private TileGroup spareTiles;
        private byte currentPlayerId;

        public void init() {
            for (int i = 0; i <= 3; i++) playerTiles[i] = PlayerData.ServerSpec.defaultInstance();
            spareTiles = Tiles.getShuffledTiles();
            currentPlayerId = (byte) new Random().nextInt(0, 4);
            if (playerData[currentPlayerId].getType() == PlayerType.EMPTY) nextPlayer();
        }

        public void destruct() {
            for (int i = 0; i <= 3; i++) playerTiles[i] = null;
            spareTiles = null;
            currentPlayerId = 0;
        }

        public PlayerData.ServerSpec[] getPlayerTiles() {
            return playerTiles;
        }

        public TileGroup getSpareTiles() {
            return spareTiles;
        }

        public byte getCurrentPlayerId() {
            return currentPlayerId;
        }

        public byte getNextPlayerId() {
            nextPlayer();
            return currentPlayerId;
        }

        private void nextPlayer() {
            do {
                currentPlayerId = (byte) ((currentPlayerId + 1) % 4);
            } while (playerData[currentPlayerId].getType() == PlayerType.EMPTY);
        }
    }

    public static class Client extends GameData {
        private boolean yourTurn = false;
        private byte localId = 0;
        private byte currentPlayerId;
        private final PlayerData.ClientSpec[] playerTiles = new PlayerData.ClientSpec[4];

        public void init() {
            for (int i = 0; i <= 3; i++) playerTiles[i] = PlayerData.ClientSpec.defaultInstance();
            currentPlayerId = -1;
        }

        public void destruct() {
            for (int i = 0; i <= 3; i++) playerTiles[i] = null;
        }

        public PlayerData.ClientSpec[] getPlayerTiles() {
            return playerTiles;
        }

        @Override
        public void changePos(byte id1, byte id2) {
            super.changePos(id1, id2);
            if (localId == id1) localId = id2;
        }

        public void setYourTurn(boolean yourTurn) {
            this.yourTurn = yourTurn;
        }

        public boolean isYourTurn() {
            return yourTurn;
        }

        public void setLocalId(byte localId) {
            this.localId = localId;
        }

        public byte getLocalId() {
            return localId;
        }

        public void setCurrentPlayerId(byte currentPlayerId) {
            this.currentPlayerId = currentPlayerId;
        }

        public byte getCurrentPlayerId() {
            return currentPlayerId;
        }
    }
}
