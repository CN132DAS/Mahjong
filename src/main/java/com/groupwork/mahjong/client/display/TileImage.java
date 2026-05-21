package com.groupwork.mahjong.client.display;

import com.groupwork.mahjong.common.tiles.Tile;
import com.groupwork.mahjong.common.tiles.TileGroup;
import java.util.ArrayList;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class TileImage extends Group implements Comparable<TileImage> {
    public static final int WIDTH = 44;
    public static final int HEIGHT = 55;

    private static final Image[] images = new Image[35];

    private final Tile tile;

    public static void init() {
        for (int i = 0; i <= 34; i++) images[i] = new Image("tileTexture/%d.png".formatted(i));
    }

    public TileImage(Tile tile, Util.Rotation rotation, boolean interactive) {
        super();
        this.tile = tile;
        ImageView imageView = new ImageView(TileImage.images[tile.getID()]);
        imageView.setFitWidth(WIDTH);
        imageView.setFitHeight(HEIGHT);
        imageView.setRotate(rotation.degree);
        this.getChildren().add(imageView);
        if (interactive) {
            this.setPickOnBounds(true);
            this.addEventFilter(MouseEvent.MOUSE_CLICKED, Logic.onClickTile(this.tile));
        }
    }

    @Override
    public int compareTo(TileImage o) {
        return this.tile.compareTo(o.tile);
    }

    public static Pane getGroupDisplay(
            TileGroup group, Util.Rotation rotation, boolean interactive) {
        if (rotation == Util.Rotation.NO_ROTATION || rotation == Util.Rotation.REVERSE_180) {
            HBox result = new HBox();
            result.getChildren()
                    .addAll(
                            group.stream()
                                    .map(tile -> new TileImage(tile, rotation, interactive))
                                    .toList());
            return result;
        } else {
            VBox result = new VBox();
            result.getChildren()
                    .addAll(
                            group.stream()
                                    .map(tile -> new TileImage(tile, rotation, interactive))
                                    .toList());
            return result;
        }
    }

    public static ArrayList<? extends Pane> getShownDisplay(
            ArrayList<TileGroup> tileShown, Util.Rotation rotation) {
        ArrayList<Pane> result = new ArrayList<>();
        for (var group : tileShown) {
            result.add(getGroupDisplay(group, rotation, false));
        }
        return result;
    }
}
