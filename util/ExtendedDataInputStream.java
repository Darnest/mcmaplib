package mcmaplib.util;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.math.BigInteger;

public class ExtendedDataInputStream extends DataInputStream {
    public ExtendedDataInputStream(InputStream in) {
        super(in);
    }

    public short readLEUnsignedByte() throws IOException, EOFException {
        return (short)readUnsignedByte();
    }

    public int readLEUnsignedShort() throws IOException, EOFException {
        int s = 0;

        s += readUnsignedByte()        & 0x00FF;
        s += (readUnsignedByte() << 8) & 0xFF00;
        return s;
    }

    public long readLEUnsignedInt() throws IOException, EOFException {
        long i = 0;

        i += readUnsignedByte()          & 0x000000FFL;
        i += (readUnsignedByte() << 8)   & 0x0000FF00L;
        i += (readUnsignedByte() << 16)  & 0x00FF0000L;
        i += (readUnsignedByte() << 24)  & 0xFF000000L;
        return i;
    }

    public long readUnsignedInt() throws IOException, EOFException {
        long i = 0;

        i += (readUnsignedByte() << 24) & 0xFF000000L;
        i += (readUnsignedByte() << 16) & 0x00FF0000L;
        i += (readUnsignedByte() << 8)  & 0x0000FF00L;
        i += readUnsignedByte()         & 0x000000FFL;
        return i;
    }

    public BigInteger readLEUnsignedBigInteger(int size) throws IOException, EOFException {
        BigInteger num;
        byte[] data;

        data = new byte[size];

        for(int i = size - 1; i >= 0;i--) {
            int dat;

            dat = read();
            if(dat == -1)
                throw new EOFException();
            
            data[i] = (byte)dat;
        }

        num = new BigInteger(data);
        return num;
    }
}