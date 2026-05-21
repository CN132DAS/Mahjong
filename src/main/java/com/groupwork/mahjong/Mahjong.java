package com.groupwork.mahjong;

import com.groupwork.mahjong.client.display.TileImage;
import com.groupwork.mahjong.client.gameplay.GameStage;
import com.groupwork.mahjong.client.gameplay.Gameplay;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.tiles.Tiles;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Mahjong extends Application {
    public static final boolean TEST_MODE = false;
    private static Mahjong instance;
    private Gameplay gameplay;
    private MahjongServer server;
    private MahjongClient client;

    @Override
    public void init() throws Exception {
        Tiles.init();
        TileImage.init();
    }

    @Override
    public void start(Stage stage) throws Exception {
        Mahjong.instance = this;
        client = new MahjongClient();
        gameplay = new Gameplay(stage);
    }

    @Override
    public void stop() throws Exception {
        if (client.isConnected()) {
            client.sendMessage(
                    new Messages.PlayerLeave(
                            client.getGameData().getLocalId(),
                            Messages.PlayerLeave.Reason.DISCONNECT));
        }
        client.shutdown();
    }

    public void refresh() {
        Platform.runLater(gameplay::refresh);
    }

    public void setGameStage(GameStage stage) {
        Platform.runLater(() -> gameplay.setGameStage(stage));
    }

    public static Mahjong getInstance() {
        return instance;
    }

    public MahjongServer getServer() {
        return server;
    }

    public void setServer(MahjongServer server) {
        this.server = server;
    }

    public MahjongClient getClient() {
        return client;
    }

    public Gameplay getGameplay() {
        return gameplay;
    }
}
