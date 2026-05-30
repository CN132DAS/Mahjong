package com.groupwork.mahjong.common.message;

import com.groupwork.mahjong.common.player.PlayerData;
import com.groupwork.mahjong.common.player.PlayerType;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.Tiles;
import org.jetbrains.annotations.NotNull;

public class Messages {
    public record ServerInfo(byte receiveId, byte adminId, @NotNull PlayerType[] types)
            implements IMessage {
        public static ServerInfo of(byte receiveId, PlayerData[] data) {
            byte adminId = 0;
            PlayerType[] types = new PlayerType[4];
            for (int i = 0; i <= 3; i++) {
                types[i] = data[i].getType();
                if (data[i].isAdmin()) adminId = (byte) i;
            }
            return new ServerInfo(receiveId, adminId, types);
        }

        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[6];
            bytes[0] = receiveId;
            bytes[1] = adminId;
            for (int i = 0; i <= 3; i++) bytes[2 + i] = (byte) types[i].ordinal();
            return new BinaryMessage(MessageType.SERVER_INFO, bytes);
        }

        public static ServerInfo toMessage(byte[] bytes) {
            PlayerType[] playerData = new PlayerType[4];
            for (int i = 0; i <= 3; i++) playerData[i] = PlayerType.values()[bytes[2 + i]];
            return new ServerInfo(bytes[0], bytes[1], playerData);
        }
    }

    public record PlayerJoin(byte id, @NotNull PlayerType type) implements IMessage {
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

    public record PlayerLeave(byte id, @NotNull Reason reason) implements IMessage {
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

    public record PosChange(byte fromId, byte toId) implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            return new BinaryMessage(MessageType.POS_CHANGE, new byte[] {fromId, toId});
        }

        public static PosChange toMessage(byte[] bytes) {
            return new PosChange(bytes[0], bytes[1]);
        }
    }

    public record AIChange(byte id, @NotNull Type type) implements IMessage {
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

    public record GameStateEvent(@NotNull Type type, byte id1, byte id2) implements IMessage {
        public enum Type {
            GAME_START_PRE,
            GAME_START,
            //            STAGE_PRE,          //最开始抓牌后的阶段,允许:出牌、暗杠、加杠、自摸
            //            STAGE_POST,         //当有人打出过牌后的阶段,允许:跳过、出牌、吃、碰、明杠、和
            END_DRAW,
            END_WIN;
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

    public record PlayerDraw(
            byte id, boolean inHand, boolean inPreState, byte spareNum, @NotNull Tile[] tiles)
            implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            int num = tiles.length;
            byte[] bytes = new byte[4 + num];
            bytes[0] = id;
            bytes[1] = (byte) (inHand ? 1 : 0);
            bytes[2] = (byte) (inPreState ? 1 : 0);
            bytes[3] = spareNum;
            for (int i = 0; i < num; i++) bytes[4 + i] = tiles[i].getID();
            return new BinaryMessage(MessageType.PLAYER_DRAW, bytes);
        }

        public static PlayerDraw toMessage(byte[] bytes) {
            int num = bytes.length - 4;
            byte id = bytes[0];
            boolean inHand = bytes[1] == 1;
            boolean inPreState = bytes[2] == 1;
            byte spareNum = bytes[3];
            Tile[] tiles = new Tile[num];
            for (int i = 0; i < num; i++) tiles[i] = Tiles.getTile(bytes[4 + i]);
            return new PlayerDraw(id, inHand, inPreState, spareNum, tiles);
        }
    }

    public record PlayerAction(byte id, @NotNull Type type, @NotNull Tile tile)
            implements IMessage, Comparable<PlayerAction> {
        @Override
        public int compareTo(PlayerAction o) {
            final int i = type.ordinal() - o.type.ordinal();
            return switch (type) {
                case PENG -> o.type == Type.GANG ? 0 : i;
                case GANG -> o.type == Type.PENG ? 0 : i;
                default -> i;
            };
        }

        public enum Type {
            HU,
            PENG,
            GANG,
            CHI,
            DISCARD,
            SKIP
        }

        @Override
        public BinaryMessage toBinaryMessage() {
            byte[] bytes = new byte[3];
            bytes[0] = id;
            bytes[1] = (byte) type.ordinal();
            bytes[2] = tile.getID();
            return new BinaryMessage(MessageType.PLAYER_ACTION, bytes);
        }

        public static PlayerAction toMessage(byte[] bytes) {
            int num = bytes.length - 2;
            byte id = bytes[0];
            Type type = Type.values()[bytes[1]];
            Tile tile = Tiles.getTile(bytes[2]);
            return new PlayerAction(id, type, tile);
        }
    }

    public record GameTick(byte tickCount) implements IMessage {
        @Override
        public BinaryMessage toBinaryMessage() {
            return new BinaryMessage(MessageType.GAME_TICK, new byte[] {tickCount});
        }

        public static GameTick toMessage(byte[] bytes) {
            return new GameTick(bytes[0]);
        }
    }
}
