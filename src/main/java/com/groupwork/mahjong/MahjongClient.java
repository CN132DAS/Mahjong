package com.groupwork.mahjong;

import com.groupwork.mahjong.client.display.Util;
import com.groupwork.mahjong.client.gameplay.GameStage;
import com.groupwork.mahjong.common.GameData;
import com.groupwork.mahjong.common.message.BinaryMessage;
import com.groupwork.mahjong.common.message.IMessage;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.network.AbstractMessageHandler;
import com.groupwork.mahjong.common.network.Communicator;
import com.groupwork.mahjong.common.player.PlayerType;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class MahjongClient {
    //    private Thread thread;
    //    private volatile boolean running = true;

    private Communicator communicator;
    private final ClientMessageHandler messageHandler = new ClientMessageHandler();

    private GameData.Client gameData;
    private boolean admin = false;

    //    private boolean start = false;

    public boolean isAdmin() {
        return admin;
    }

    public GameData.Client getGameData() {
        return gameData;
    }

    //    @Override
    //    public void run() {
    //        thread = Thread.currentThread();
    //        while(running&&thread.isInterrupted()){
    //            while (!start)
    //                try {
    //                    Thread.sleep(200);
    //                } catch (InterruptedException e) {
    //                    if(!running)
    //                        break;
    //                }
    //            while (start){
    //
    //            }
    //        }
    //    }

    public int connect(InetAddress address, boolean isAdmin) {
        try {
            Socket server = new Socket(address, MahjongServer.PORT);
            communicator = new Communicator(server, messageHandler.receivedMessage);
            new Thread(communicator, "client-communicator-thread").start();
            new Thread(messageHandler, "client-messageHandler-thread").start();
        } catch (ConnectException e) {
            return -1;
        } catch (SocketTimeoutException _) {
            return -2;
        } catch (IOException _) {
            return -3;
        }
        gameData = new GameData.Client();
        admin = isAdmin;
        return 0;
    }

    public void disconnect() {
        gameData.destruct();
        communicator.shutdown();
        communicator = null;
        Mahjong.getInstance().setGameStage(GameStage.MAIN_MENU);
    }

    public boolean isConnected() {
        return communicator != null;
    }

    public void shutdown() {
        messageHandler.shutdown();
        //        running = false;
        //        thread.interrupt();
    }

    public void sendMessage(IMessage message) {
        if (communicator != null) communicator.sendMessage(message.toBinaryMessage());
    }

    private class ClientMessageHandler extends AbstractMessageHandler {
        @Override
        public void processMessage(BinaryMessage binaryMessage) {
            switch (binaryMessage.type()) {
                case SERVER_INFO -> {
                    Messages.ServerInfo message_ =
                            Messages.ServerInfo.toMessage(binaryMessage.bytes());
                    gameData.setPlayerData(message_.playerData());
                    gameData.getPlayerData()[message_.receiveId()].setType(PlayerType.LOCAL);
                    gameData.setLocalId(message_.receiveId());
                    Mahjong.getInstance().refresh();
                }
                case PLAYER_JOIN -> {
                    Messages.PlayerJoin message_ =
                            Messages.PlayerJoin.toMessage(binaryMessage.bytes());
                    if (message_.id() == gameData.getLocalId()) return;
                    gameData.getPlayerData()[message_.id()].setType(message_.type());
                    Mahjong.getInstance().refresh();
                }
                case PLAYER_LEAVE -> {
                    Messages.PlayerLeave message_ =
                            Messages.PlayerLeave.toMessage(binaryMessage.bytes());
                    switch (message_.reason()) {
                        case DISCONNECT -> {
                            gameData.getPlayerData()[message_.id()].setType(PlayerType.EMPTY);
                            if (message_.id() == gameData.getLocalId()) disconnect();
                            else Mahjong.getInstance().refresh();
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
                    Messages.PosChange message_ =
                            Messages.PosChange.toMessage(binaryMessage.bytes());
                    byte id1 = message_.id1();
                    byte id2 = message_.id2();
                    gameData.changePos(id1, id2);
                    Mahjong.getInstance().refresh();
                }
                case AI_CHANGE -> {
                    Messages.AIChange message_ = Messages.AIChange.toMessage(binaryMessage.bytes());
                    switch (message_.type()) {
                        case SET ->
                                gameData.getPlayerData()[message_.id()].setType(PlayerType.FAKE);
                        case CLEAR ->
                                gameData.getPlayerData()[message_.id()].setType(PlayerType.EMPTY);
                    }
                    Mahjong.getInstance().refresh();
                }
                case GAME_STATE_EVENT -> {
                    Messages.GameStateEvent message_ =
                            Messages.GameStateEvent.toMessage(binaryMessage.bytes());
                    switch (message_.type()) {
                        case START -> {
                            gameData.init();
                            Mahjong.getInstance().setGameStage(GameStage.PLAYING);
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
                            Mahjong.getInstance().setGameStage(GameStage.SERVER_LOBBY);
                        }
                    }
                }
                case PLAYER_DRAW -> {
                    Messages.PlayerDraw message_ =
                            Messages.PlayerDraw.toMessage(binaryMessage.bytes());
                    var spec = gameData.getPlayerTiles()[message_.id()];
                    if (message_.num() == 1) {
                        if (message_.inHand()) spec.tileInHand.add(message_.tiles()[0]);
                        else spec.tileDraw = message_.tiles()[0];
                    } else {
                        spec.tileInHand.addAll(Arrays.asList(message_.tiles()));
                    }
                    Mahjong.getInstance().refresh();
                }
                case PLAYER_TURN -> {
                    Messages.PLayerTurn message_ =
                            Messages.PLayerTurn.toMessage(binaryMessage.bytes());
                    gameData.setCurrentPlayerId(message_.id());
                    gameData.setYourTurn(message_.id() == gameData.getLocalId());
                    Mahjong.getInstance().refresh();
                }
                case PLAYER_ACTION -> {
                    Messages.PlayerAction message_ =
                            Messages.PlayerAction.toMessage(binaryMessage.bytes());
                    switch (message_.type()) {
                        case DISCARD -> {
                            var playerTile = gameData.getPlayerTiles()[message_.id()];
                            var tile = message_.tiles()[0];
                            if (message_.id() == gameData.getLocalId()) {
                                if (tile != playerTile.tileDraw) {
                                    playerTile.tileInHand.remove(tile);
                                    playerTile.tileInHand.add(playerTile.tileDraw);
                                }
                                playerTile.tileDraw = null;
                            } else {
                                if (playerTile.tileDraw != null) playerTile.tileDraw = null;
                                else playerTile.tileInHand.remove(tile);
                            }
                            playerTile.tileDiscarded.add(tile);
                            Mahjong.getInstance().refresh();
                        }
                    }
                }
            }
            ;
        }
    }
}
