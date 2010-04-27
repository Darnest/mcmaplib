package mcmaplib.util;

import java.math.BigInteger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;

public class ExtendedDataOutputStream extends DataOutputStream {
    public ExtendedDataOutputStream(OutputStream out) {
        super(out);
    }

    public void writeUnsignedByte(short b) throws IOException {
        writeByte((byte)b);
    }

    public void writeUnsignedShort(int s) throws IOException {
        writeShort((short)s);
    }

    public void writeLEUnsignedByte(short b) throws IOException {
        write(b & 0xFF);
    }

    public void writeLEUnsignedShort(int s) throws IOException {
        write(s & 0xFF);
        write((s >> 8) & 0xFF);
    }

    public void writeLEUnsignedInt(long i) throws IOException {
        write((int)(i & 0xFF));
        write((int)((i >> 8) & 0xFF));
        write((int)((i >> 16) & 0xFF));
        write((int)((i >> 24) & 0xFF));
    }

    public void writeUnsignedInt(long i) throws IOException {
        writeInt((int)i);
    }

    public void writeLEUnsignedBigInteger(BigInteger num, int size) throws IOException {
        byte[] data;
        int stop;

        data = num.toByteArray();
        stop = data.length - size;
        for(int i = data.length - 1; i >= stop;i--) {
            if(i < 0)
                write(0);
            else
                write(data[i]);
        }
    }
}