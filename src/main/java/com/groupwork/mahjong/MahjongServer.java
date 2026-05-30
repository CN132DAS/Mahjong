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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MahjongServer implements Runnable {
    public static final int PORT = 25364;
    public static final byte TICK_COUNT = 30;

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
            gameData.init();
            gameLoop();
            gameData.destruct();
            listenerHandler.listening = true;
        }
    }

    private void gameLoop() {
        gamePreDraw();
        podcast(
                new Messages.GameStateEvent(
                        Messages.GameStateEvent.Type.GAME_START, (byte) -1, (byte) -1));
        drawTile(false, true, 1, false);
        while (!gameData.getSpareTiles().isEmpty() && running) {
            Messages.PlayerAction messagePre = actionBeforeDiscard();
            if (!running || messagePre == null) return;
            Tile messageTile = messagePre.tile();
            switch (messagePre.type()) {
                case DISCARD -> {
                    gameData.handleDiscard(messageTile);
                    podcast(messagePre);
                }
                case GANG -> {
                    if (!gameData.handleJiaGang(messageTile)) gameData.handleAnGang(messageTile);
                    podcast(messagePre);
                    drawTile(false, true, 1, true);
                    continue;
                }
                case HU -> {
                    byte playerId = gameData.getCurrentPlayerId();
                    podcast(
                            new Messages.GameStateEvent(
                                    Messages.GameStateEvent.Type.END_WIN, playerId, playerId));
                    return;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                if (!running) return;
            }
            postLoop:
            while (true) {
                Messages.PlayerAction messagePost = actionAfterDiscard();
                switch (messagePost.type()) {
                    case CHI -> {
                        gameData.handleChi(messagePost.id(), messagePost.tile());
                        podcast(messagePost);
                    }
                    case PENG -> {
                        gameData.handlePeng(messagePost.id());
                        podcast(messagePost);
                    }
                    case GANG -> {
                        gameData.handleNormalGang(messagePost.id());
                        podcast(messagePost);
                        drawTile(false, false, 1, true);
                    }
                    case HU -> {
                        podcast(
                                new Messages.GameStateEvent(
                                        Messages.GameStateEvent.Type.END_WIN,
                                        messagePost.id(),
                                        gameData.getCurrentPlayerId()));
                        return;
                    }
                    case SKIP -> {
                        break postLoop;
                    }
                }
                Messages.PlayerAction discardMessage = waitForTileToDiscard();
                gameData.handleDiscard(discardMessage.tile());
                podcast(discardMessage);
            }
            gameData.nextPlayer();
            drawTile(false, true, 1, false);
        }
        podcast(
                new Messages.GameStateEvent(
                        Messages.GameStateEvent.Type.END_DRAW, (byte) -1, (byte) -1));
    }

    private void drawTile(boolean inHand, boolean inPreState, int num, boolean reverse) {
        byte playerId = gameData.getCurrentPlayerId();
        Tile[] tiles = gameData.getSpareTiles().drawTiles(num, false);
        Tile[] wildcardTiles = new Tile[num];
        for (int i = 0; i < num; i++) {
            gameData.getPlayerData()[playerId].tileInHand.add(tiles[i]);
            wildcardTiles[i] = Tiles.WILDCARD;
        }
        byte spareNum = (byte) gameData.getSpareTiles().size();
        post(new Messages.PlayerDraw(playerId, inHand, inPreState, spareNum, tiles), playerId);
        filteredPodcast(
                new Messages.PlayerDraw(playerId, inHand, inPreState, spareNum, wildcardTiles),
                playerId);
    }

    private void gamePreDraw() {
        for (int round = 1; round <= 3; round++)
            for (int i = 0; i < listenerHandler.currentPlayers.get(); i++) {
                drawTile(true, true, 4, false);
                gameData.nextPlayer();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    if (!running) return;
                }
            }
        for (int i = 0; i < listenerHandler.currentPlayers.get(); i++) {
            drawTile(true, true, 1, false);
            gameData.nextPlayer();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                if (!running) return;
            }
        }
    }

    private Messages.PlayerAction actionBeforeDiscard() {
        byte currentPlayerId = gameData.getCurrentPlayerId();
        var currentPlayerData = gameData.getPlayerData()[currentPlayerId];
        switch (currentPlayerData.getType()) {
            case FAKE -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (!running) return null;
                }
                Tile tile = FakePlayerUtil.calculateTileToDiscard(currentPlayerData.tileInHand);
                return new Messages.PlayerAction(
                        currentPlayerId, Messages.PlayerAction.Type.DISCARD, tile);
            }
            case REMOTE -> {
                byte tickChance = TICK_COUNT; // 30秒思考时间
                try {
                    Messages.PlayerAction message = null;
                    while (tickChance >= 0) {
                        post(new Messages.GameTick(tickChance), currentPlayerId);
                        message = inGameMessage.poll(1, TimeUnit.SECONDS);
                        tickChance--;
                        if (message != null) {
                            switch (message.type()) {
                                case GANG, DISCARD -> {
                                    inGameMessage.clear();
                                    return message;
                                }
                            }
                        }
                    }
                } catch (InterruptedException _) {
                    if (!running) return null;
                }
            }
        }
        inGameMessage.clear();
        Tile tile = FakePlayerUtil.calculateTileToDiscard(currentPlayerData.tileInHand);
        return new Messages.PlayerAction(currentPlayerId, Messages.PlayerAction.Type.DISCARD, tile);
    }

    private Messages.PlayerAction actionAfterDiscard() {
        boolean[] actions = new boolean[] {false, false, false, false};
        int totalNum = 0;
        byte tickChance = TICK_COUNT;
        for (int i = 0; i <= 3; i++)
            if (gameData.getPlayerData()[i].getType() == PlayerType.REMOTE) totalNum++;
        ArrayList<Messages.PlayerAction> messages = new ArrayList<>(4);
        while (totalNum > 0 && tickChance >= 0 || !inGameMessage.isEmpty()) {
            podcast(new Messages.GameTick(TICK_COUNT));
            try {
                Messages.PlayerAction message = inGameMessage.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    if (!actions[message.id()]) {
                        actions[message.id()] = true;
                        totalNum--;
                        messages.add(message);
                    }
                    continue;
                }
            } catch (InterruptedException e) {
                if (!running)
                    return new Messages.PlayerAction(
                            (byte) -1, Messages.PlayerAction.Type.SKIP, Tiles.WILDCARD);
            }
            tickChance--;
        }
        Collections.sort(messages);
        if (messages.isEmpty())
            return new Messages.PlayerAction(
                    (byte) -1, Messages.PlayerAction.Type.SKIP, Tiles.WILDCARD);
        else {
            return messages.getFirst();
        }
    }

    private Messages.PlayerAction waitForTileToDiscard() {
        byte currentPlayerId = gameData.getCurrentPlayerId();
        var currentPlayerData = gameData.getPlayerData()[currentPlayerId];
        switch (currentPlayerData.getType()) {
            case FAKE -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (!running) return null;
                }
                Tile tile = FakePlayerUtil.calculateTileToDiscard(currentPlayerData.tileInHand);
                return new Messages.PlayerAction(
                        currentPlayerId, Messages.PlayerAction.Type.DISCARD, tile);
            }
            case REMOTE -> {
                byte tickChance = TICK_COUNT; // 30秒思考时间
                try {
                    Messages.PlayerAction message = null;
                    while (tickChance >= 0) {
                        post(new Messages.GameTick(tickChance), currentPlayerId);
                        message = inGameMessage.poll(1, TimeUnit.SECONDS);
                        tickChance--;
                        if (message != null
                                && message.type() == Messages.PlayerAction.Type.DISCARD) {
                            inGameMessage.clear();
                            return message;
                        }
                    }
                } catch (InterruptedException _) {
                    if (!running) return null;
                }
            }
        }
        inGameMessage.clear();
        Tile tile = FakePlayerUtil.calculateTileToDiscard(currentPlayerData.tileInHand);
        return new Messages.PlayerAction(currentPlayerId, Messages.PlayerAction.Type.DISCARD, tile);
    }

    private synchronized void addPlayer(boolean isAdmin, Socket clientSocket) throws IOException {
        var players = gameData.getPlayerData();
        for (int i = 0; i <= 3; i++)
            if (!clients[i].isConnected() && players[i].getType() == PlayerType.EMPTY) {
                clients[i].connect(clientSocket);
                players[i].setType(PlayerType.REMOTE);
                players[i].setAdmin(isAdmin);
                clients[i].sendMessage(Messages.ServerInfo.of((byte) i, players));
                podcast(new Messages.PlayerJoin((byte) i, PlayerType.REMOTE));
                return;
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
            thread = Thread.currentThread();
            try (serverSocket) {
                serverSocket.setSoTimeout(100);
                while (running) {
                    try {
                        int current = currentPlayers.get();
                        if (current >= MAX_PLAYERS) {
                            thread.sleep(100);
                            continue;
                        }
                        if (listening) {
                            Socket clientSocket = serverSocket.accept();
                            int num = currentPlayers.incrementAndGet();
                            addPlayer(num == 1, clientSocket);
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
                    Messages.PlayerLeave message =
                            Messages.PlayerLeave.toMessage(binaryMessage.bytes());
                    switch (message.reason()) {
                        case KICKED -> {
                            byte id = message.id();
                            filteredPodcast(
                                    new Messages.PlayerLeave(
                                            id, Messages.PlayerLeave.Reason.DISCONNECT),
                                    message.id());
                            post(
                                    new Messages.PlayerLeave(
                                            id, Messages.PlayerLeave.Reason.KICKED),
                                    message.id());
                            clients[id].disconnect();
                            gameData.getPlayerData()[id].setType(PlayerType.EMPTY);
                            listenerHandler.currentPlayers.decrementAndGet();
                        }
                        case DISCONNECT -> {
                            byte id = message.id();
                            if (!gameData.getPlayerData()[id].isAdmin()) {
                                podcast(message);
                                clients[id].disconnect();
                                gameData.getPlayerData()[id].setType(PlayerType.EMPTY);
                                listenerHandler.currentPlayers.decrementAndGet();
                            } else {
                                filteredPodcast(
                                        new Messages.PlayerLeave(
                                                (byte) -1,
                                                Messages.PlayerLeave.Reason.SERVER_CLOSE),
                                        id);
                                post(message, id);
                                MahjongServer.this.shutdown();
                            }
                        }
                    }
                }
                case POS_CHANGE -> {
                    Messages.PosChange message =
                            Messages.PosChange.toMessage(binaryMessage.bytes());
                    byte id1 = message.fromId();
                    byte id2 = message.toId();
                    gameData.changePos(id1, id2);
                    clients[id1].setId(id2);
                    clients[id2].setId(id1);
                    Arrays.sort(clients);
                    podcast(message);
                }
                case AI_CHANGE -> {
                    Messages.AIChange message = Messages.AIChange.toMessage(binaryMessage.bytes());
                    switch (message.type()) {
                        case SET -> {
                            gameData.getPlayerData()[message.id()].setType(PlayerType.FAKE);
                            listenerHandler.currentPlayers.incrementAndGet();
                        }
                        case CLEAR -> {
                            gameData.getPlayerData()[message.id()].setType(PlayerType.EMPTY);
                            listenerHandler.currentPlayers.decrementAndGet();
                        }
                    }
                    podcast(message);
                }
                case GAME_STATE_EVENT -> {
                    Messages.GameStateEvent message =
                            Messages.GameStateEvent.toMessage(binaryMessage.bytes());
                    switch (message.type()) {
                        case GAME_START_PRE -> {
                            start = true;
                            podcast(message);
                        }
                    }
                }
                case PLAYER_ACTION -> {
                    Messages.PlayerAction message =
                            Messages.PlayerAction.toMessage(binaryMessage.bytes());
                    inGameMessage.offer(message);
                }
            }
        }
    }
}
