package com.groupwork.mahjong.client.gameplay;

import java.util.function.Supplier;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public enum GameStage {
    MAIN_MENU(Screen::setMainMenu),
    SERVER_LOBBY(Screen::setServerLobby),
    PLAYING(Screen::setInGame);

    private final Supplier<Parent> renderLogic;

    GameStage(Supplier<Parent> renderLogic) {
        this.renderLogic = renderLogic;
    }

    public void render(Stage stage) {
        Scene scene = stage.getScene();
        if (scene != null) {
            double width = stage.getWidth();
            double height = stage.getHeight();
            Parent parent = renderLogic.get();
            Scene newScene = new Scene(parent);
            width = Math.max(width, newScene.getWidth());
            height = Math.max(height, newScene.getHeight());
            stage.setScene(newScene);
            stage.setWidth(width);
            stage.setHeight(height);
        } else {
            stage.setScene(new Scene(renderLogic.get()));
        }
    }
}
