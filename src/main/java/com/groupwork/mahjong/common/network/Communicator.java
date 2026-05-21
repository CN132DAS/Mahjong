package com.groupwork.mahjong.common.network;

import com.groupwork.mahjong.common.message.BinaryMessage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Communicator implements Runnable {
    private Thread thread = null;
    private volatile boolean running = true;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final BlockingQueue<BinaryMessage> binaryMessageToSend = new LinkedBlockingQueue<>();
    private final BlockingQueue<BinaryMessage> binaryMessageReceived;

    public Communicator(Socket socket, BlockingQueue<BinaryMessage> binaryMessageReceived)
            throws IOException {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        this.binaryMessageReceived = binaryMessageReceived;
        this.socket.setSoTimeout(50);
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        while (running && !socket.isClosed() && !thread.isInterrupted()) {
            try {
                try {
                    BinaryMessage binaryMessage = BinaryMessage.staticRead(in);
                    binaryMessageReceived.offer(binaryMessage);
                } catch (SocketTimeoutException _) {
                }
                BinaryMessage binaryMessage = binaryMessageToSend.poll(50, TimeUnit.MILLISECONDS);
                if (binaryMessage != null) BinaryMessage.write(out, binaryMessage);
            } catch (InterruptedException e) {
                thread.interrupt();
            } catch (IOException e) {
                break;
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            thread.join(1000);
        } catch (InterruptedException _) {
        }
        try {
            socket.close();
        } catch (IOException _) {
        }
    }

    public void sendMessage(BinaryMessage msg) {
        binaryMessageToSend.offer(msg);
    }

    public void setThreadName(String name) {
        if (thread != null) thread.setName(name);
    }
}
