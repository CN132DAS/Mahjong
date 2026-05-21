package com.groupwork.mahjong.client.display;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Util {
    public static Text textWithFont(String text, double size) {
        return textWithFont(text, size, null);
    }

    public static Text textWithFont(String text, double size, String fontName) {
        Text result = new Text(text);
        result.setFont(new Font(fontName, size));
        return result;
    }

    public static Button buttonWithFont(String text, double size) {
        return buttonWithFont(text, size, null);
    }

    public static Button buttonWithFont(String text, double size, String fontName) {
        Button result = new Button(text);
        result.setFont(new Font(fontName, size));
        return result;
    }

    public static void showAlert(
            Alert.AlertType type, String title, String headerText, String contentText) {
        Alert alert = new Alert(type);
        if (title != null) alert.setTitle(title);
        if (headerText != null) alert.setHeaderText(headerText);
        if (contentText != null) alert.setContentText(contentText);
        alert.showAndWait();
    }

    public static HBox getDiscardedTilesHBox() {
        HBox result = new HBox();
        result.setMinSize(6 * TileImage.WIDTH, TileImage.HEIGHT);
        result.setMaxSize(6 * TileImage.WIDTH, TileImage.HEIGHT);
        result.setAlignment(Pos.CENTER_LEFT);
        return result;
    }

    public enum Rotation {
        NO_ROTATION(0),
        REVERSE_90(-90),
        REVERSE_180(-180),
        REVERSE_270(-270);

        public final double degree;

        Rotation(double degree) {
            this.degree = degree;
        }
    }
}
