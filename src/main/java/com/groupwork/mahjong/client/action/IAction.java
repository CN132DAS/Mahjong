package com.groupwork.mahjong.client.action;

import com.groupwork.mahjong.MahjongClient;
import com.groupwork.mahjong.client.display.Util;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.Tiles;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

public interface IAction {
    String getText();

    EventHandler<MouseEvent> getEventHandler();

    default Button asActionButton() {
        Button button = Util.buttonWithFont(getText(), 25);
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, getEventHandler());
        return button;
    }

    static IAction Chi(Tile[] tiles) {
        return new Chi(tiles);
    }

    static IAction Peng(Tile[] tiles) {
        return new Peng(tiles);
    }

    static IAction Gang(Tile[] tiles) {
        return new Gang(tiles);
    }

    IAction HU =
            new IAction() {
                @Override
                public String getText() {
                    return "胡";
                }

                @Override
                public EventHandler<MouseEvent> getEventHandler() {
                    return event -> {
                        MahjongClient client = MahjongClient.getInstance();
                        client.sendLocalMessage(
                                id ->
                                        new Messages.PlayerAction(
                                                (byte) id,
                                                Messages.PlayerAction.Type.HU,
                                                Tiles.WILDCARD));
                    };
                }
            };
    IAction SKIP =
            new IAction() {
                @Override
                public String getText() {
                    return "跳过";
                }

                @Override
                public EventHandler<MouseEvent> getEventHandler() {
                    return event -> {
                        MahjongClient client = MahjongClient.getInstance();
                        client.sendLocalMessage(
                                id ->
                                        new Messages.PlayerAction(
                                                (byte) id,
                                                Messages.PlayerAction.Type.SKIP,
                                                Tiles.WILDCARD));
                    };
                }
            };
}
