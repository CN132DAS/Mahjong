package com.groupwork.mahjong;

import com.groupwork.mahjong.common.GameData;
import com.groupwork.mahjong.common.message.BinaryMessage;
import com.groupwork.mahjong.common.message.IMessage;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.network.*;
import com.groupwork.mahjong.common.player.FakePlayerUtil;
import com.groupwork.mahjong.common.player.PlayerType;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.Tiles;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MahjongServer implements Runnable {
    public static final int PORT = 25364;

    private Thread thread;
    private volatile boolean running = true;

    private final ServerMessageHandler messageHandler = new ServerMessageHandler();
    private final ListenerHandler listenerHandler;
    private final ClientHandler[] clients = new ClientHandler[4];

    private final GameData.Server gameData = new GameData.Server();
    private final BlockingQueue<Messages.PlayerAction> inGameMessage = new LinkedBlockingQueue<>();
    private volatile boolean start = false;

    {
        for (int i = 0; i <= 3; i++) clients[i] = new ClientHandler((byte) i);
    }

    public MahjongServer() throws IOException {
        ServerSocket listeningSocket = new ServerSocket(PORT);
        listenerHandler = new ListenerHandler(listeningSocket);
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        new Thread(messageHandler, "Server-messageHandler-thread").start();
        new Thread(listenerHandler, "Server-listener-thread").start();
        while (running && !thread.isInterrupted()) {
            start = false;
            while (!start)
                try {
                    Thread.sleep(400);
                } catch (InterruptedException _) {
                    if (!running) break;
                }
            if (!running) break;
            listenerHandler.listening = false;
            game();
            listenerHandler.listening = true;
        }
    }

    private void game() {
        gameData.init();
        byte currentPlayerId = gameData.getCurrentPlayerId();
        for (int round = 1; round <= 3; round++)
            for (int i = 0; i < listenerHandler.currentPlayers.get(); i++) {
                drawTile(currentPlayerId, true, 4);
                currentPlayerId = gameData.getNextPlayerId();
            }
        for (int i = 0; i < listenerHandler.currentPlayers.get(); i++) {
            drawTile(currentPlayerId, true, 1);
            currentPlayerId = gameData.getNextPlayerId();
        }
        while (!gameData.getSpareTiles().isEmpty()) {
            drawTile(currentPlayerId, false, 1);
            podcast(new Messages.PLayerTurn(currentPlayerId));
            switch (gameData.getPlayerData()[currentPlayerId].getType()) {
                case FAKE -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        if (!running) return;
                    }
                    Tile tile =
                            FakePlayerUtil.calculateTileToDiscard(
                                    gameData.getPlayerTiles()[currentPlayerId].tileInHand);
                    gameData.getPlayerTiles()[currentPlayerId].tileInHand.remove(tile);
                    gameData.getPlayerTiles()[currentPlayerId].tileDiscarded.add(tile);
                    podcast(
                            new Messages.PlayerAction(
                                    currentPlayerId,
                                    Messages.PlayerAction.Type.DISCARD,
                                    (byte) 1,
                                    new Tile[] {tile}));
                }
                case REMOTE -> {
                    boolean flag = true;
                    while (flag) {
                        try {
                            Messages.PlayerAction message =
                                    inGameMessage.poll(30, TimeUnit.SECONDS);
                            if (message == null) {
                                Tile tile =
                                        FakePlayerUtil.calculateTileToDiscard(
                                                gameData.getPlayerTiles()[currentPlayerId]
                                                        .tileInHand);
                                podcast(
                                        new Messages.PlayerAction(
                                                currentPlayerId,
                                                Messages.PlayerAction.Type.DISCARD,
                                                (byte) 1,
                                                new Tile[] {tile}));
                                flag = false;
                            } else if (message.id() == currentPlayerId) {
                                if (message.type() == Messages.PlayerAction.Type.DISCARD) {
                                    Tile tile = message.tiles()[0];
                                    gameData.getPlayerTiles()[currentPlayerId].tileInHand.remove(
                                            tile);
                                    gameData.getPlayerTiles()[currentPlayerId].tileDiscarded.add(
                                            tile);
                                    podcast(
                                            new Messages.PlayerAction(
                                                    currentPlayerId,
                                                    Messages.PlayerAction.Type.DISCARD,
                                                    (byte) 1,
                                                    message.tiles()));
                                    flag = false;
                                }
                            }
                        } catch (InterruptedException _) {
                            if (!running) return;
                        }
                    }
                }
            }
            currentPlayerId = gameData.getNextPlayerId();
        }
        podcast(
                new Messages.GameStateEvent(
                        Messages.GameStateEvent.Type.END_DRAW, (byte) -1, (byte) -1));
        gameData.destruct();
    }

    private void drawTile(byte playerId, boolean inHand, int num) {
        Tile[] tiles = gameData.getSpareTiles().drawTiles(num);
        Tile[] wildcardTiles = new Tile[num];
        for (int i = 0; i < num; i++) {
            gameData.getPlayerTiles()[playerId].tileInHand.add(tiles[i]);
            wildcardTiles[i] = Tiles.WILDCARD;
        }
        post(new Messages.PlayerDraw(playerId, inHand, (byte) num, tiles), playerId);
        filteredPodcast(
                new Messages.PlayerDraw(playerId, inHand, (byte) num, wildcardTiles), playerId);
    }

    private synchronized void addPlayer(boolean isAdmin, Socket clientSocket) throws IOException {
        var players = gameData.getPlayerData();
        for (int i = 0; i <= 3; i++)
            if (!clients[i].isConnected() && players[i].getType() == PlayerType.EMPTY) {
                clients[i].connect(clientSocket);
                players[i].setType(PlayerType.REMOTE);
                players[i].setAdmin(isAdmin);
                clients[i].sendMessage(new Messages.ServerInfo((byte) i, players));
                podcast(new Messages.PlayerJoin((byte) i, PlayerType.REMOTE));
                break;
            }
    }

    private void filteredPodcast(IMessage message, byte filteredId) {
        for (int i = 0; i <= 3; i++) {
            if (i == filteredId) continue;
            post(message, (byte) i);
        }
    }

    private void podcast(IMessage message) {
        for (int i = 0; i <= 3; i++) post(message, (byte) i);
    }

    private void post(IMessage message, byte id) {
        if (clients[id].isConnected()) clients[id].sendMessage(message);
    }

    public void shutdown() {
        for (var client : clients) client.disconnect();
        listenerHandler.shutdown();
        messageHandler.shutdown();
        running = false;
        thread.interrupt();
    }

    private class ListenerHandler implements Runnable {
        private static final int MAX_PLAYERS = 4;

        private Thread thread;
        private volatile boolean running = true;

        private final AtomicInteger currentPlayers;
        private final ServerSocket serverSocket;

        private volatile boolean listening = true;

        public ListenerHandler(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            currentPlayers = new AtomicInteger(0);
        }

        @Override
        public void run() {
            try (serverSocket) {
                serverSocket.setSoTimeout(100);
                while (running) {
                    try {
                        int current = currentPlayers.get();
                        if (current >= MAX_PLAYERS) {
                            continue;
                        }
                        if (listening) {
                            Socket clientSocket = serverSocket.accept();
                            currentPlayers.incrementAndGet();
                            addPlayer(currentPlayers.get() == 1, clientSocket);
                        }
                        Thread.sleep(500);
                    } catch (SocketTimeoutException _) {
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (IOException _) {
            }
        }

        public void shutdown() {
            this.running = false;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    private class ClientHandler implements Comparable<ClientHandler> {
        private byte id;
        private Communicator communicator;

        public void connect(Socket clientSocket) throws IOException {
            communicator = new Communicator(clientSocket, messageHandler.receivedMessage);
            new Thread(communicator, "server-communicator-thread" + id).start();
            if (Mahjong.TEST_MODE) System.out.println("Client " + id + " connected to the server.");
        }

        public void disconnect() {
            if (communicator == null) return;
            communicator.shutdown();
            communicator = null;
        }

        public ClientHandler(Byte id) {
            this.id = id;
        }

        public boolean isConnected() {
            return communicator != null;
        }

        public void sendMessage(IMessage message) {
            if (Mahjong.TEST_MODE) System.out.println("Server has message to send to client " + id);
            communicator.sendMessage(message.toBinaryMessage());
        }

        public void setId(byte id) {
            this.id = id;
            if (this.communicator != null)
                this.communicator.setThreadName("server-communicator-thread" + id);
        }

        @Override
        public int compareTo(ClientHandler o) {
            return this.id - o.id;
        }
    }

    private class ServerMessageHandler extends AbstractMessageHandler {
        @Override
        public void processMessage(BinaryMessage binaryMessage) {
            switch (binaryMessage.type()) {
                case PLAYER_LEAVE -> {
                    Messages.PlayerLeave message_ =
                            Messages.PlayerLeave.toMessage(binaryMessage.bytes());
                    switch (message_.reason()) {
                        case KICKED -> {
                            byte id = message_.id();
                            filteredPodcast(
                                    new Messages.PlayerLeave(
                                            id, Messages.PlayerLeave.Reason.DISCONNECT),
                                    message_.id());
                            post(
                                    new Messages.PlayerLeave(
                                            id, Messages.PlayerLeave.Reason.KICKED),
                                    message_.id());
                            clients[id].disconnect();
                            gameData.getPlayerData()[id].setType(PlayerType.EMPTY);
                            listenerHandler.currentPlayers.decrementAndGet();
                        }
                        case DISCONNECT -> {
                            byte id = message_.id();
                            if (!gameData.getPlayerData()[id].isAdmin()) {
                                podcast(message_);
                                clients[id].disconnect();
                                gameData.getPlayerData()[id].setType(PlayerType.EMPTY);
                                listenerHandler.currentPlayers.decrementAndGet();
                            } else {
                                filteredPodcast(
                                        new Messages.PlayerLeave(
                                                (byte) -1,
                                                Messages.PlayerLeave.Reason.SERVER_CLOSE),
                                        id);
                                post(message_, id);
                                MahjongServer.this.shutdown();
                            }
                        }
                    }
                }
                case POS_CHANGE -> {
                    Messages.PosChange message_ =
                            Messages.PosChange.toMessage(binaryMessage.bytes());
                    byte id1 = message_.id1();
                    byte id2 = message_.id2();
                    gameData.changePos(id1, id2);
                    clients[id1].setId(id2);
                    clients[id2].setId(id1);
                    Arrays.sort(clients);
                    podcast(message_);
                }
                case AI_CHANGE -> {
                    Messages.AIChange message_ = Messages.AIChange.toMessage(binaryMessage.bytes());
                    switch (message_.type()) {
                        case SET -> {
                            gameData.getPlayerData()[message_.id()].setType(PlayerType.FAKE);
                            listenerHandler.currentPlayers.incrementAndGet();
                        }
                        case CLEAR -> {
                            gameData.getPlayerData()[message_.id()].setType(PlayerType.EMPTY);
                            listenerHandler.currentPlayers.decrementAndGet();
                        }
                    }
                    podcast(message_);
                }
                case GAME_STATE_EVENT -> {
                    Messages.GameStateEvent message_ =
                            Messages.GameStateEvent.toMessage(binaryMessage.bytes());
                    switch (message_.type()) {
                        case START -> {
                            start = true;
                            podcast(message_);
                        }
                    }
                }
                case PLAYER_ACTION -> {
                    Messages.PlayerAction message_ =
                            Messages.PlayerAction.toMessage(binaryMessage.bytes());
                    inGameMessage.offer(message_);
                }
            }
        }
    }
}
