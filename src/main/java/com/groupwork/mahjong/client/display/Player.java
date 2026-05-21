package com.groupwork.mahjong.client.display;

import com.groupwork.mahjong.Mahjong;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.player.PlayerData;
import java.util.ArrayList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Player {

    public static HBox getInfoDisplay(PlayerData data, byte id) {
        return switch (data.getType()) {
            case LOCAL -> PlayerInfoDisplayHelper.local(data, id);
            case REMOTE -> PlayerInfoDisplayHelper.remote(data, id);
            case FAKE -> PlayerInfoDisplayHelper.fake(data, id);
            case EMPTY -> PlayerInfoDisplayHelper.empty(data, id);
        };
    }

    public static Pane getTilesDisplay(
            PlayerData.ClientSpec data, Util.Rotation rotation, boolean interactive) {
        if (rotation == Util.Rotation.NO_ROTATION || rotation == Util.Rotation.REVERSE_180) {
            ArrayList<? extends Pane> tilesShown =
                    TileImage.getShownDisplay(data.tileShown, Util.Rotation.NO_ROTATION);

            HBox result = new HBox();
            result.setAlignment(Pos.CENTER);
            result.setSpacing(20);
            result.getChildren().addAll(tilesShown);
            result.getChildren()
                    .add(TileImage.getGroupDisplay(data.tileInHand, rotation, interactive));
            if (data.tileDraw != null) {
                result.getChildren().add(new TileImage(data.tileDraw, rotation, interactive));
            }
            if (rotation == Util.Rotation.REVERSE_180) result.setRotate(-180);
            return result;
        } else {
            ArrayList<? extends Pane> tilesShown =
                    TileImage.getShownDisplay(data.tileShown, Util.Rotation.REVERSE_90);

            VBox result = new VBox();
            result.setAlignment(Pos.CENTER);
            result.setSpacing(20);
            result.getChildren().addAll(tilesShown);
            result.getChildren()
                    .add(TileImage.getGroupDisplay(data.tileInHand, rotation, interactive));
            if (data.tileDraw != null) {
                result.getChildren().add(new TileImage(data.tileDraw, rotation, interactive));
            }
            if (rotation == Util.Rotation.REVERSE_270) result.setRotate(-180);
            return result;
        }
    }

    public static VBox getDiscardedTilesDisplay(String name, PlayerData.ClientSpec data, byte id) {
        Text text = Util.textWithFont(name, 20);

        VBox result = new VBox(text);
        result.setAlignment(Pos.CENTER);
        result.setMinSize(6 * TileImage.WIDTH, 5 * TileImage.HEIGHT + 20);

        HBox row = Util.getDiscardedTilesHBox();
        int count = 0;
        for (var tile : data.tileDiscarded) {
            row.getChildren().add(new TileImage(tile, Util.Rotation.NO_ROTATION, false));
            count++;
            if (count == 6) {
                result.getChildren().add(row);
                VBox.setVgrow(row, Priority.NEVER);
                row = Util.getDiscardedTilesHBox();
                count = 0;
            }
        }
        if (count > 0) {
            result.getChildren().add(row);
            VBox.setVgrow(row, Priority.NEVER);
        }
        return result;
    }

    private static class PlayerInfoDisplayHelper {

        private static HBox getDescriptionOutline() {
            HBox hBox = new HBox();
            hBox.setMinHeight(80);
            hBox.setMinWidth(300);
            hBox.setBorder(
                    new Border(
                            new BorderStroke(
                                    Color.BLACK,
                                    BorderStrokeStyle.SOLID,
                                    new CornerRadii(5),
                                    new BorderWidths(2))));
            hBox.setPadding(new Insets(0, 20, 0, 20));
            hBox.setAlignment(Pos.CENTER);
            return hBox;
        }

        public static HBox local(PlayerData data, byte id) {
            Text text = Util.textWithFont(data.getDescriptionString(id), 20);

            Region spacing = new Region();
            HBox.setHgrow(spacing, Priority.ALWAYS);

            Button disconnect = Util.buttonWithFont("断开连接", 20);
            disconnect.addEventHandler(MouseEvent.MOUSE_CLICKED, Logic.leave(id));

            HBox hBox = getDescriptionOutline();
            hBox.getChildren().addAll(text, spacing, disconnect);
            return hBox;
        }

        public static HBox remote(PlayerData data, byte id) {
            Text text = Util.textWithFont(data.getDescriptionString(id), 20);

            Region spacing = new Region();
            HBox.setHgrow(spacing, Priority.ALWAYS);

            HBox hBox = getDescriptionOutline();
            if (Mahjong.getInstance().getClient().isAdmin()) {
                Button kick = Util.buttonWithFont("踢出", 20);
                kick.addEventHandler(MouseEvent.MOUSE_CLICKED, Logic.kick(id));

                hBox.getChildren().addAll(text, spacing, kick);
            } else {
                hBox.getChildren().addAll(text, spacing);
            }
            return hBox;
        }

        public static HBox fake(PlayerData data, byte id) {
            Text text = Util.textWithFont(data.getDescriptionString(id), 20);

            Region spacing = new Region();
            HBox.setHgrow(spacing, Priority.ALWAYS);

            HBox hBox = getDescriptionOutline();
            if (Mahjong.getInstance().getClient().isAdmin()) {
                Button clearAI = Util.buttonWithFont("清除", 20);
                clearAI.addEventHandler(
                        MouseEvent.MOUSE_CLICKED,
                        Logic.aiRelevant(id, Messages.AIChange.Type.CLEAR));

                hBox.getChildren().addAll(text, spacing, clearAI);
            } else {
                hBox.getChildren().addAll(text, spacing);
            }
            return hBox;
        }

        public static HBox empty(PlayerData data, byte id) {
            Text text = Util.textWithFont(data.getDescriptionString(id), 20);

            Region spacing = new Region();
            HBox.setHgrow(spacing, Priority.ALWAYS);

            Button changePos = Util.buttonWithFont("切换至此", 20);
            changePos.addEventHandler(MouseEvent.MOUSE_CLICKED, Logic.changePos(id));

            HBox hBox = getDescriptionOutline();
            if (Mahjong.getInstance().getClient().isAdmin()) {
                Button setAI = Util.buttonWithFont("设为AI", 20);
                setAI.addEventHandler(
                        MouseEvent.MOUSE_CLICKED, Logic.aiRelevant(id, Messages.AIChange.Type.SET));

                HBox buttonGroup = new HBox(setAI, changePos);
                buttonGroup.setAlignment(Pos.CENTER);
                buttonGroup.setSpacing(10);

                hBox.getChildren().addAll(text, spacing, buttonGroup);
            } else {
                hBox.getChildren().addAll(text, spacing, changePos);
            }
            return hBox;
        }
    }
}
