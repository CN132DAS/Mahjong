package com.groupwork.mahjong.client.display;

import com.groupwork.mahjong.Mahjong;
import com.groupwork.mahjong.MahjongClient;
import com.groupwork.mahjong.MahjongServer;
import com.groupwork.mahjong.client.gameplay.GameStage;
import com.groupwork.mahjong.common.message.Messages;
import com.groupwork.mahjong.common.tiles.Tile;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;

public class Logic {
    public static void createServer(MouseEvent event) {
        try {
            MahjongServer server = new MahjongServer();
            new Thread(server, "server-thread").start();
            Mahjong.getInstance().setServer(server);
        } catch (IOException e) {
            Util.showAlert(Alert.AlertType.ERROR, "服务器创建失败", "请重试", null);
            return;
        }
        while (true) {
            int result =
                    Mahjong.getInstance()
                            .getClient()
                            .connect(InetAddress.ofLiteral("127.0.0.1"), true);
            if (result == 0) break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException _) {
            }
        }
        Mahjong.getInstance().setGameStage(GameStage.SERVER_LOBBY);
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
                    address = InetAddress.ofLiteral(result.get());
                    break;
                } catch (IllegalArgumentException _) {
                    Util.showAlert(Alert.AlertType.ERROR, "错误", null, "IP地址无效!");
                }
            } else return;
        }
        int result = Mahjong.getInstance().getClient().connect(address, false);
        if (result != 0) {
            Util.showAlert(Alert.AlertType.ERROR, "错误", "连接失败!", "网络超时或服务器不存在");
            return;
        }
        Mahjong.getInstance().getGameplay().setGameStage(GameStage.SERVER_LOBBY);
    }

    public static void start(MouseEvent event) {
        MahjongClient client = Mahjong.getInstance().getClient();
        client.sendMessage(
                new Messages.GameStateEvent(
                        Messages.GameStateEvent.Type.START, (byte) -1, (byte) -1));
    }

    public static EventHandler<MouseEvent> leave(byte id) {
        return event -> {
            MahjongClient client = Mahjong.getInstance().getClient();
            client.sendMessage(
                    new Messages.PlayerLeave(id, Messages.PlayerLeave.Reason.DISCONNECT));
        };
    }

    public static EventHandler<MouseEvent> kick(byte id) {
        return event -> {
            MahjongClient client = Mahjong.getInstance().getClient();
            client.sendMessage(new Messages.PlayerLeave(id, Messages.PlayerLeave.Reason.KICKED));
        };
    }

    public static EventHandler<MouseEvent> aiRelevant(byte id, Messages.AIChange.Type type) {
        return event -> {
            MahjongClient client = Mahjong.getInstance().getClient();
            client.sendMessage(new Messages.AIChange(id, type));
        };
    }

    public static EventHandler<MouseEvent> changePos(byte id) {
        return event -> {
            MahjongClient client = Mahjong.getInstance().getClient();
            client.sendMessage(new Messages.PosChange(client.getGameData().getLocalId(), id));
        };
    }

    public static EventHandler<MouseEvent> onClickTile(Tile tile) {
        return event -> {
            MahjongClient client = Mahjong.getInstance().getClient();
            client.sendMessage(
                    new Messages.PlayerAction(
                            client.getGameData().getLocalId(),
                            Messages.PlayerAction.Type.DISCARD,
                            (byte) 1,
                            new Tile[] {tile}));
        };
    }

//    public static AnimationTimer getTimer(){
//        return new AnimationTimer() {
//            long lastTime
//            @Override
//            public void handle(long now) {
//
//            }
//        }
//    }
}
