package com.groupwork.mahjong.common.message;

import com.groupwork.mahjong.common.player.PlayerData;
import com.groupwork.mahjong.common.player.PlayerType;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.Tiles;

public class Messages {
    public record ServerInfo(byte receiveId, PlayerData[] playerData) implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[9];
            bytes[0] = receiveId;
            for (int i = 0; i <= 3; i++) {
                bytes[i * 2 + 1] = (byte) (playerData[i].isAdmin() ? 1 : 0);
                bytes[i * 2 + 2] = (byte) playerData[i].getType().ordinal();
            }
            return new BinaryMessage(MessageType.SERVER_INFO, bytes);
        }

        public static ServerInfo toMessage(byte[] bytes) {
            byte[] id = new byte[] {1, 2, 3, 4};
            PlayerData[] playerData = new PlayerData[4];
            for (int i = 0; i <= 3; i++) {
                playerData[i] = new PlayerData();
                playerData[i].setAdmin(bytes[2 * i + 1] == 1);
                playerData[i].setType(PlayerType.values()[bytes[2 * i + 2]]);
            }
            return new ServerInfo(bytes[0], playerData);
        }
    }

    public record PlayerJoin(byte id, PlayerType type) implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[2];
            bytes[0] = id;
            bytes[1] = (byte) type.ordinal();
            return new BinaryMessage(MessageType.PLAYER_JOIN, bytes);
        }

        public static PlayerJoin toMessage(byte[] bytes) {
            return new PlayerJoin(bytes[0], PlayerType.values()[bytes[1]]);
        }
    }

    public record PlayerLeave(byte id, Reason reason) implements IMessage {
        public enum Reason {
            DISCONNECT,
            KICKED,
            SERVER_CLOSE
        }

        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[] {id, (byte) reason.ordinal()};
            return new BinaryMessage(MessageType.PLAYER_LEAVE, bytes);
        }

        public static PlayerLeave toMessage(byte[] bytes) {
            return new PlayerLeave(bytes[0], Reason.values()[bytes[1]]);
        }
    }

    public record PosChange(byte id1, byte id2) implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            return new BinaryMessage(MessageType.POS_CHANGE, new byte[] {id1, id2});
        }

        public static PosChange toMessage(byte[] bytes) {
            return new PosChange(bytes[0], bytes[1]);
        }
    }

    public record AIChange(byte id, Type type) implements IMessage {
        public enum Type {
            SET,
            CLEAR;
        }

        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[] {id, (byte) type.ordinal()};
            return new BinaryMessage(MessageType.AI_CHANGE, bytes);
        }

        public static AIChange toMessage(byte[] bytes) {
            return new AIChange(bytes[0], Type.values()[bytes[1]]);
        }
    }

    public record GameStateEvent(Type type, byte id1, byte id2) implements IMessage {
        public enum Type {
            START,
            END_DRAW,
            END_WIN_SELF,
            END_WIN_OTHER;
        }

        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[] {(byte) type.ordinal(), id1, id2};
            return new BinaryMessage(MessageType.GAME_STATE_EVENT, bytes);
        }

        public static GameStateEvent toMessage(byte[] bytes) {
            return new GameStateEvent(Type.values()[bytes[0]], bytes[1], bytes[2]);
        }
    }

    public record PlayerDraw(byte id, boolean inHand, byte num, Tile[] tiles) implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[3 + num];
            bytes[0] = id;
            bytes[1] = inHand ? (byte) 1 : (byte) 0;
            bytes[2] = num;
            for (int i = 0; i < num; i++) bytes[3 + i] = tiles[i].getID();
            return new BinaryMessage(MessageType.PLAYER_DRAW, bytes);
        }

        public static PlayerDraw toMessage(byte[] bytes) {
            byte id = bytes[0];
            boolean inHand = bytes[1] == 1;
            byte num = bytes[2];
            Tile[] tiles = new Tile[num];
            for (int i = 0; i < num; i++) tiles[i] = Tiles.getTile(bytes[3 + i]);
            return new PlayerDraw(id, inHand, num, tiles);
        }
    }

    public record PLayerTurn(byte id) implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            return new BinaryMessage(MessageType.PLAYER_TURN, new byte[] {id});
        }

        public static PLayerTurn toMessage(byte[] bytes) {
            return new PLayerTurn(bytes[0]);
        }
    }

    public record PlayerAction(byte id, Type type, byte num, Tile[] tiles) implements IMessage {
        public enum Type {
            DISCARD,
            CHI,
            PENG,
            MING_GANG,
            AN_GANG,
            JIA_GANG,
            HU,
            SKIP
        }

        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[3 + num];
            bytes[0] = id;
            bytes[1] = (byte) type.ordinal();
            bytes[2] = num;
            for (int i = 0; i < num; i++) bytes[3 + i] = tiles[i].getID();
            return new BinaryMessage(MessageType.PLAYER_ACTION, bytes);
        }

        public static PlayerAction toMessage(byte[] bytes) {
            byte id = bytes[0];
            Type type = Type.values()[bytes[1]];
            byte num = bytes[2];
            Tile[] tiles = new Tile[num];
            for (int i = 0; i < num; i++) tiles[i] = Tiles.getTile(bytes[3 + i]);
            return new PlayerAction(id, type, num, tiles);
        }
    }

    public record WaitingForChoice(Type type) {
        public enum Type {
            CHI,
            PENG,
            MING_GANG,
            AN_GANG,
            JIA_GANG,
            HU,
        }
    }
}
