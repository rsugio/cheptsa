package io.rsug.cheptsa;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
    ByteBuffer bb;
    ByteBufferOutputStream(ByteBuffer b) {
        bb = b;
    }

    @Override
    public void write(int i) throws IOException {
        if (!bb.hasRemaining()) flush();
        byte b = (byte) i;
        bb.put(b);
    }
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (bb.remaining() < len) flush();
        bb.put(b, off, len);
    }
}
