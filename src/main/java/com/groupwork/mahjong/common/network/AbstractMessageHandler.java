package com.groupwork.mahjong.common.network;

import com.groupwork.mahjong.common.message.BinaryMessage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractMessageHandler implements Runnable {
    private Thread thread = null;
    private volatile boolean running = true;
    public final BlockingQueue<BinaryMessage> receivedMessage =
            new LinkedBlockingQueue<BinaryMessage>();

    @Override
    public void run() {
        thread = Thread.currentThread();
        while (running && !thread.isInterrupted()) {
            try {
                processMessage(receivedMessage.take());
            } catch (InterruptedException e) {
                thread.interrupt();
            }
        }
    }

    public void shutdown() {
        this.running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public abstract void processMessage(BinaryMessage binaryMessage);
}
