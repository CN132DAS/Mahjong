package com.groupwork.mahjong.common.message;

import com.groupwork.mahjong.Mahjong;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public record BinaryMessage(MessageType type, byte[] bytes) {

    @Override
    public String toString() {
        return type.toString() + Arrays.toString(bytes);
    }

    public static void write(DataOutputStream out, BinaryMessage binaryMessage) throws IOException {
        out.writeByte(binaryMessage.type.ordinal());
        int size = binaryMessage.bytes.length;
        out.writeByte(size);
        if (size > 0) out.write(binaryMessage.bytes);
        out.flush();
        if (Mahjong.TEST_MODE)
            System.out.println(
                    Thread.currentThread().getName() + ": Message sent: " + binaryMessage);
    }

    public static BinaryMessage staticRead(DataInputStream in) throws IOException {
        MessageType type = MessageType.values()[in.readByte()];
        int size = (in.readByte() & 0xFF);
        byte[] information = null;
        if (size > 0) {
            information = new byte[size];
            in.read(information);
        }
        BinaryMessage binaryMessage = new BinaryMessage(type, information);
        if (Mahjong.TEST_MODE)
            System.out.println(
                    Thread.currentThread().getName() + ": Message received: " + binaryMessage);
        return binaryMessage;
    }
}
