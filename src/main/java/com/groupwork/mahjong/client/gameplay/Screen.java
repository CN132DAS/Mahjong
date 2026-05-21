package com.groupwork.mahjong.client.gameplay;

import com.groupwork.mahjong.Mahjong;
import com.groupwork.mahjong.client.display.Logic;
import com.groupwork.mahjong.client.display.Player;
import com.groupwork.mahjong.client.display.Util;
import com.groupwork.mahjong.common.GameData;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

class Screen {
    static Parent setMainMenu() {
        Text title = Util.textWithFont("麻将\nMahjong", 80);
        title.setTextAlignment(TextAlignment.CENTER);

        Button create = Util.buttonWithFont("创建", 30);
        create.addEventFilter(MouseEvent.MOUSE_CLICKED, Logic::createServer);

        Button connect = Util.buttonWithFont("加入", 30);
        connect.addEventFilter(MouseEvent.MOUSE_CLICKED, Logic::connectServer);

        VBox content = new VBox(title, create, connect);
        content.setAlignment(Pos.CENTER);
        content.setSpacing(20);

        return content;
    }

    static Parent setServerLobby() {
        Region spacing0 = new Region();
        VBox.setVgrow(spacing0, Priority.ALWAYS);

        Text title = Util.textWithFont("房间大厅", 60);
        title.setTextAlignment(TextAlignment.CENTER);

        Region spacing1 = new Region();
        VBox.setVgrow(spacing1, Priority.ALWAYS);

        VBox playerList = new VBox();
        playerList.setSpacing(50);
        byte i = 0;
        for (var player : Mahjong.getInstance().getClient().getGameData().getPlayerData()) {
            playerList.getChildren().add(Player.getInfoDisplay(player, i));
            i++;
        }
        playerList.setBorder(
                new Border(
                        new BorderStroke(
                                Color.BLACK,
                                BorderStrokeStyle.SOLID,
                                new CornerRadii(5),
                                new BorderWidths(2))));
        playerList.setAlignment(Pos.CENTER);
        playerList.setMaxWidth(500);
        playerList.setMaxHeight(500);

        VBox content = new VBox(spacing0, title, spacing1, playerList);
        content.setAlignment(Pos.CENTER);
        content.setSpacing(20);

        try {
            HBox ipDisplay = new HBox();
            ipDisplay.setAlignment(Pos.CENTER);
            ipDisplay.setPadding(new Insets(10, 10, 10, 10));

            Text ip = Util.textWithFont("ip:" + InetAddress.getLocalHost().getHostAddress(), 30);

            Region spacing2 = new Region();
            HBox.setHgrow(spacing2, Priority.ALWAYS);
            if (Mahjong.getInstance().getClient().isAdmin()) {
                Button start = Util.buttonWithFont("开始游戏", 30);
                start.addEventHandler(MouseEvent.MOUSE_CLICKED, Logic::start);

                ipDisplay.getChildren().addAll(ip, spacing2, start);
            } else {
                ipDisplay.getChildren().addAll(ip, spacing2);
            }

            Region spacing3 = new Region();
            VBox.setVgrow(spacing3, Priority.ALWAYS);

            content.getChildren().addAll(spacing3, ipDisplay);
        } catch (UnknownHostException _) {
        }
        return content;
    }

    static Parent setInGame() {
        GameData.Client data = Mahjong.getInstance().getClient().getGameData();
        byte localId = data.getLocalId();
        byte currentPlayerId = data.getCurrentPlayerId();
        Pane[] tiles = new Pane[4];
        for (int i = 0; i <= 3; i++)
            tiles[i] =
                    Player.getTilesDisplay(
                            data.getPlayerTiles()[(localId + i) % 4],
                            Util.Rotation.values()[i],
                            i == 0 && data.isYourTurn());

        BorderPane result = new BorderPane();
        result.setBottom(tiles[0]);
        result.setRight(tiles[1]);
        result.setTop(tiles[2]);
        result.setLeft(tiles[3]);

        VBox[] discardedTiles = new VBox[4];
        for (int i = 0; i <= 3; i++) {
            byte id = (byte) ((localId + i) % 4);
            discardedTiles[i] =
                    Player.getDiscardedTilesDisplay(
                            data.getPlayerData()[id].getDescriptionString(id),
                            data.getPlayerTiles()[id],
                            id);
        }

        HBox center = new HBox();
        center.setPadding(new Insets(10, 10, 10, 10));
        Region centerSpacing1 = new Region();
        HBox.setHgrow(centerSpacing1, Priority.ALWAYS);
        Region centerSpacing2 = new Region();
        HBox.setHgrow(centerSpacing2, Priority.ALWAYS);

        VBox centerMiddle = new VBox();
        String currentPlayer =
                currentPlayerId == -1
                        ? "无"
                        : data.getPlayerData()[currentPlayerId].getDescriptionString(
                                currentPlayerId);
        Text roundInfo = Util.textWithFont("当前回合:\n" + currentPlayer, 15);
        roundInfo.setTextAlignment(TextAlignment.CENTER);
        VBox centerMiddleSpacing = new VBox(roundInfo);
        centerMiddleSpacing.setAlignment(Pos.CENTER);

        VBox.setVgrow(centerMiddleSpacing, Priority.ALWAYS);
        centerMiddle
                .getChildren()
                .addAll(discardedTiles[2], centerMiddleSpacing, discardedTiles[0]);

        center.getChildren()
                .addAll(
                        discardedTiles[3],
                        centerSpacing1,
                        centerMiddle,
                        centerSpacing2,
                        discardedTiles[1]);
        result.setCenter(center);

        return result;
    }
}
