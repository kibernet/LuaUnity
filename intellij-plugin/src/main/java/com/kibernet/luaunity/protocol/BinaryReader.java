package com.kibernet.luaunity.protocol;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

final class BinaryReader {
    private final ByteBuffer buffer;

    BinaryReader(byte[] bytes) {
        buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    int available() {
        return buffer.remaining();
    }

    boolean readBoolean() throws EOFException {
        return readByte() != 0;
    }

    byte readByte() throws EOFException {
        ensure(1);
        return buffer.get();
    }

    int readInt() throws EOFException {
        ensure(4);
        return buffer.getInt();
    }

    String readString() throws EOFException {
        int length = readInt();
        if (length < 0) {
            throw new EOFException("Negative string length.");
        }
        ensure(length);
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void ensure(int length) throws EOFException {
        if (buffer.remaining() < length) {
            throw new EOFException("Unexpected end of LuaUnity payload.");
        }
    }
}
