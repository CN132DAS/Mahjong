package com.groupwork.mahjong.client.display;

import com.groupwork.mahjong.MahjongClient;
import com.groupwork.mahjong.MahjongServer;
import com.groupwork.mahjong.client.action.PossibleAction;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.tiles.Tile;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;

public class Logic {
    public static void createServer(MouseEvent event) {
        try {
            MahjongServer server = new MahjongServer();
            new Thread(server, "server-thread").start();
        } catch (IOException e) {
            Util.showAlert(Alert.AlertType.ERROR, "服务器创建失败", "请重试", null);
            return;
        }
        while (true) {
            int result =
                    MahjongClient.getInstance().connect(InetAddress.getLoopbackAddress());
            if (result == 0) break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void connectServer(MouseEvent event) {
        InetAddress address;
        while (true) {
            TextInputDialog dialog = new TextInputDialog("127.0.0.1");
            dialog.setTitle("连接至服务器");
            dialog.setHeaderText("请输入IP地址:");
            dialog.setContentText("例:xxx.xxx.xxx.xx");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    address = InetAddress.getByName(result.get());
                    break;
                } catch (IOException ignored) {
                    Util.showAlert(Alert.AlertType.ERROR, "错误", null, "IP地址无效!");
                }
            } else return;
        }
        int result = MahjongClient.getInstance().connect(address);
        if (result != 0) {
            Util.showAlert(Alert.AlertType.ERROR, "错误", "连接失败!", "网络超时或服务器不存在");
        }
    }

    public static void start(MouseEvent event) {
        MahjongClient client = MahjongClient.getInstance();
        client.sendMessage(
                new Messages.GameStateEvent(
                        Messages.GameStateEvent.Type.GAME_START_PRE, (byte) -1, (byte) -1));
    }

    public static EventHandler<MouseEvent> leave(byte id) {
        return event -> {
            MahjongClient client = MahjongClient.getInstance();
            client.sendMessage(
                    new Messages.PlayerLeave(id, Messages.PlayerLeave.Reason.DISCONNECT));
        };
    }

    public static EventHandler<MouseEvent> kick(byte id) {
        return event -> {
            MahjongClient client = MahjongClient.getInstance();
            client.sendMessage(new Messages.PlayerLeave(id, Messages.PlayerLeave.Reason.KICKED));
        };
    }

    public static EventHandler<MouseEvent> aiRelevant(byte id, Messages.AIChange.Type type) {
        return event -> {
            MahjongClient client = MahjongClient.getInstance();
            client.sendMessage(new Messages.AIChange(id, type));
        };
    }

    public static EventHandler<MouseEvent> changePos(byte id) {
        return event -> {
            MahjongClient client = MahjongClient.getInstance();
            client.sendLocalMessage(id1 -> new Messages.PosChange((byte) id1, id));
        };
    }

    public static EventHandler<MouseEvent> onClickTile(Tile tile) {
        return event -> {
            MahjongClient client = MahjongClient.getInstance();
            client.sendLocalMessage(
                    id ->
                            new Messages.PlayerAction(
                                    (byte) id, Messages.PlayerAction.Type.DISCARD, tile));
        };
    }

    public static List<Button> actionButtons(PossibleAction actions) {
        List<Button> buttons = new ArrayList<>();
        for (var action : actions.actions()) {
            buttons.add(action.asActionButton());
        }
        return buttons;
    }
}
