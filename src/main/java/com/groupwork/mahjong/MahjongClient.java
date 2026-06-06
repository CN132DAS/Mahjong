package com.groupwork.mahjong;

import com.groupwork.mahjong.client.action.PossibleAction;
import com.groupwork.mahjong.client.display.Util;
import com.groupwork.mahjong.client.gameplay.GameStage;
import com.groupwork.mahjong.common.GameData;
import com.groupwork.mahjong.common.message.BinaryMessage;
import com.groupwork.mahjong.common.message.IMessage;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.network.AbstractMessageHandler;
import com.groupwork.mahjong.common.network.Communicator;
import com.groupwork.mahjong.common.player.PlayerType;
import com.groupwork.mahjong.common.tiles.Tiles;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.function.IntFunction;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class MahjongClient {
    private static MahjongClient instance;

    private Communicator communicator;
    private final ClientMessageHandler messageHandler = new ClientMessageHandler();

    private GameData.Client gameData;

    public MahjongClient() {
        MahjongClient.instance = this;
    }

    public static MahjongClient getInstance() {
        return instance;
    }

    public GameData.Client getGameData() {
        return gameData;
    }

    public int connect(InetAddress address, boolean isAdmin) {
        try {
            Socket server = new Socket(address, MahjongServer.PORT);
            communicator = new Communicator(server, messageHandler.receivedMessage);
            new Thread(communicator, "client-communicator-thread").start();
            new Thread(messageHandler, "client-messageHandler-thread").start();
        } catch (ConnectException e) {
            return -1;
        } catch (SocketTimeoutException ignored) {
            return -2;
        } catch (IOException ignored) {
            return -3;
        }
        gameData = new GameData.Client();
        return 0;
    }

    public void disconnect() {
        gameData.destruct();
        communicator.shutdown();
        communicator = null;
        Mahjong.setGameStage(GameStage.MAIN_MENU);
    }

    public boolean isConnected() {
        return communicator != null;
    }

    public void shutdown() {
        messageHandler.shutdown();
    }

    public void sendMessage(IMessage message) {
        if (communicator != null) communicator.sendMessage(message.toBinaryMessage());
    }

    public void sendLocalMessage(IntFunction<IMessage> messageProvider) {
        sendMessage(messageProvider.apply(gameData.getLocalId()));
    }

    private class ClientMessageHandler extends AbstractMessageHandler {
        @Override
        public void processMessage(BinaryMessage binaryMessage) {
            switch (binaryMessage.type()) {
                case SERVER_INFO -> {
                    Messages.ServerInfo message =
                            Messages.ServerInfo.toMessage(binaryMessage.bytes());
                    gameData.bind(message);
                    Mahjong.setGameStage(GameStage.SERVER_LOBBY);
                }
                case PLAYER_JOIN -> {
                    Messages.PlayerJoin message =
                            Messages.PlayerJoin.toMessage(binaryMessage.bytes());
                    if (message.id() == gameData.getLocalId()) return;
                    gameData.getPlayerData()[message.id()].setType(message.type());
                    Mahjong.refresh();
                }
                case PLAYER_LEAVE -> {
                    Messages.PlayerLeave message =
                            Messages.PlayerLeave.toMessage(binaryMessage.bytes());
                    switch (message.reason()) {
                        case DISCONNECT -> {
                            gameData.getPlayerData()[message.id()].setType(PlayerType.EMPTY);
                            if (message.id() == gameData.getLocalId()) disconnect();
                            else Mahjong.refresh();
                        }
                        case SERVER_CLOSE -> {
                            disconnect();
                            Platform.runLater(
                                    () ->
                                            Util.showAlert(
                                                    Alert.AlertType.INFORMATION,
                                                    "断开连接",
                                                    null,
                                                    "房主关闭了服务器"));
                        }
                        case KICKED -> {
                            disconnect();
                            Platform.runLater(
                                    () ->
                                            Util.showAlert(
                                                    Alert.AlertType.INFORMATION,
                                                    "断开连接",
                                                    null,
                                                    "房主将你提出了服务器"));
                        }
                    }
                }
                case POS_CHANGE -> {
                    Messages.PosChange message =
                            Messages.PosChange.toMessage(binaryMessage.bytes());
                    byte id1 = message.fromId();
                    byte id2 = message.toId();
                    gameData.changePos(id1, id2);
                    Mahjong.refresh();
                }
                case AI_CHANGE -> {
                    Messages.AIChange message = Messages.AIChange.toMessage(binaryMessage.bytes());
                    switch (message.type()) {
                        case SET -> gameData.getPlayerData()[message.id()].setType(PlayerType.FAKE);
                        case CLEAR ->
                                gameData.getPlayerData()[message.id()].setType(PlayerType.EMPTY);
                    }
                    Mahjong.refresh();
                }
                case GAME_STATE_EVENT -> {
                    Messages.GameStateEvent message =
                            Messages.GameStateEvent.toMessage(binaryMessage.bytes());
                    switch (message.type()) {
                        case GAME_START_PRE -> {
                            gameData.init();
                            Mahjong.setGameStage(GameStage.PLAYING);
                        }
                        case GAME_START -> gameData.setStart(true);
                        case END_WIN -> {
                            byte id1 = message.id1();
                            byte id2 = message.id2();
                            if (id1 == id2) {
                                Platform.runLater(
                                        () ->
                                                Util.showAlert(
                                                        Alert.AlertType.INFORMATION,
                                                        "游戏结束",
                                                        "%s自摸!"
                                                                .formatted(
                                                                        gameData
                                                                                .getPlayerDescription(
                                                                                        id1)),
                                                        null));
                            } else {
                                Platform.runLater(
                                        () ->
                                                Util.showAlert(
                                                        Alert.AlertType.INFORMATION,
                                                        "游戏结束",
                                                        "%s荣和,%s点炮!"
                                                                .formatted(
                                                                        gameData
                                                                                .getPlayerDescription(
                                                                                        id1),
                                                                        gameData
                                                                                .getPlayerDescription(
                                                                                        id2)),
                                                        null));
                            }
                            gameData.destruct();
                            Mahjong.setGameStage(GameStage.SERVER_LOBBY);
                        }
                        case END_DRAW -> {
                            Platform.runLater(
                                    () ->
                                            Util.showAlert(
                                                    Alert.AlertType.INFORMATION,
                                                    "游戏结束",
                                                    "流局!",
                                                    null));
                            gameData.destruct();
                            Mahjong.setGameStage(GameStage.SERVER_LOBBY);
                        }
                    }
                }
                case PLAYER_DRAW -> {
                    Messages.PlayerDraw message =
                            Messages.PlayerDraw.toMessage(binaryMessage.bytes());
                    var spec = gameData.getPlayerData()[message.id()];
                    gameData.setSpareNum(message.spareNum());
                    spec.tileInHand.addAll(Arrays.stream(message.tiles()).toList());
                    if (!message.inHand()) spec.tileDraw = message.tiles()[0];
                    if (gameData.isStart()) {
                        gameData.setCurrentPlayerId(message.id());
                        gameData.setBeforeDiscard(message.inPreState());
                    }
                    if (gameData.isYourTurn()) {
                        if (message.inPreState())
                            gameData.setPossibleAction(
                                    PossibleAction.beforeDiscard(
                                            gameData.getLocalPlayerData(), gameData.getSpareNum()));
                        else
                            gameData.setPossibleAction(
                                    PossibleAction.afterGangDraw(gameData.getLocalPlayerData()));
                    }
                    Mahjong.refresh();
                }
                case PLAYER_ACTION -> {
                    Messages.PlayerAction message =
                            Messages.PlayerAction.toMessage(binaryMessage.bytes());
                    switch (message.type()) {
                        case DISCARD -> {
                            gameData.handleDiscard(message.tile());
                            gameData.setBeforeDiscard(false);
                            if (gameData.getCurrentPlayerId() == gameData.getLocalId())
                                gameData.setPossibleAction(null);
                            else
                                gameData.setPossibleAction(
                                        PossibleAction.afterDiscard(
                                                gameData.getLocalPlayerData(),
                                                message.tile(),
                                                gameData.canChi(),
                                                gameData.getSpareNum()));
                            if (gameData.getPossibleAction() == null)
                                sendLocalMessage(
                                        id ->
                                                new Messages.PlayerAction(
                                                        (byte) id,
                                                        Messages.PlayerAction.Type.SKIP,
                                                        Tiles.WILDCARD));
                            Mahjong.refresh();
                        }
                        case GANG -> {
                            if (gameData.isBeforeDiscard()) {
                                if (!gameData.handleJiaGang(message.tile()))
                                    gameData.handleAnGang(message.tile());
                            } else {
                                gameData.handleNormalGang(message.id());
                            }
                            gameData.setPossibleAction(null);
                            Mahjong.refresh();
                        }
                        case CHI -> {
                            gameData.handleChi(message.id(), message.tile());
                            gameData.setPossibleAction(null);
                            Mahjong.refresh();
                        }
                        case PENG -> {
                            gameData.handlePeng(message.id());
                            gameData.setPossibleAction(null);
                            Mahjong.refresh();
                        }
                    }
                }
                case GAME_TICK -> {
                    Messages.GameTick message = Messages.GameTick.toMessage(binaryMessage.bytes());
                    gameData.setTickCount(message.tickCount());
                    Mahjong.refresh();
                }
            }
            ;
        }
    }
}
