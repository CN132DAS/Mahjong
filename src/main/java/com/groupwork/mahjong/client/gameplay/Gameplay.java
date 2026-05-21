package com.groupwork.mahjong.client.gameplay;

import javafx.stage.Stage;

public class Gameplay {
    private final Stage stage;
    private GameStage gameStage = GameStage.MAIN_MENU;

    public Gameplay(Stage stage) {
        this.stage = stage;
        this.stage.setMinHeight(1000);
        this.stage.setMinWidth(1200);
        this.stage.setTitle("Mahjong");
        refresh();
        this.stage.show();
    }

    public void setGameStage(GameStage gameStage) {
        this.gameStage = gameStage;
        refresh();
    }

    public void refresh() {
        gameStage.render(stage);
    }
}
