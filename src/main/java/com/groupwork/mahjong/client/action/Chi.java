package com.groupwork.mahjong.client.action;

import com.groupwork.mahjong.MahjongClient;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.tiles.Tile;
import java.util.Optional;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;

public record Chi(Tile[] tiles) implements IAction {

    @Override
    public String getText() {
        return "吃";
    }

    @Override
    public EventHandler<MouseEvent> getEventHandler() {
        return event -> {
            MahjongClient client = MahjongClient.getInstance();
            int length = tiles.length;
            if (length == 1) {
                client.sendLocalMessage(
                        id ->
                                new Messages.PlayerAction(
                                        (byte) id, Messages.PlayerAction.Type.CHI, tiles[0]));
            } else {
                ButtonType[] buttons = new ButtonType[length];
                for (int i = 0; i < length; i++) {
                    String text = "";
                    for (var tile : tiles[i].getChiCombination())
                        text = text + tile.getShortDescription();
                    text = text + tiles[i].tileType().name;
                    buttons[i] = new ButtonType(tiles[i].getFullDescription());
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.getButtonTypes().setAll(buttons);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    ButtonType value = result.get();
                    for (int i = 0; i < length; i++)
                        if (value == buttons[i]) {
                            int finalI = i;
                            client.sendLocalMessage(
                                    id ->
                                            new Messages.PlayerAction(
                                                    (byte) id,
                                                    Messages.PlayerAction.Type.CHI,
                                                    tiles[finalI]));
                            break;
                        }
                }
            }
        };
    }
}
