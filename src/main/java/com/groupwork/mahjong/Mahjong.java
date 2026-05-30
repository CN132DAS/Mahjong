package com.groupwork.mahjong;

import com.groupwork.mahjong.client.display.TileImage;
import com.groupwork.mahjong.client.gameplay.GameStage;
import com.groupwork.mahjong.client.gameplay.Gameplay;
import com.groupwork.mahjong.common.message.Messages;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Mahjong extends Application {
    public static final boolean TEST_MODE = false;
    private static Mahjong instance;
    private Gameplay gameplay;
    private MahjongClient client;

    @Override
    public void init() throws Exception {
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
        if (client.isConnected())
            client.sendLocalMessage(
                    id ->
                            new Messages.PlayerLeave(
                                    (byte) id, Messages.PlayerLeave.Reason.DISCONNECT));
        client.shutdown();
    }

    public static void refresh() {
        Platform.runLater(instance.gameplay::refresh);
    }

    public static void setGameStage(GameStage stage) {
        Platform.runLater(() -> Mahjong.instance.gameplay.setGameStage(stage));
    }

    public static Mahjong getInstance() {
        return instance;
    }
}
