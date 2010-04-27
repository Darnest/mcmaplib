package mcmaplib;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.math.BigInteger;
import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import mcmaplib.util.ExtendedDataInputStream;
import mcmaplib.util.ExtendedDataOutputStream;

public class RUMMinecraftMap implements Cloneable, Serializable {
    private static final int MAX_WIDTH = 65535,
                             MAX_HEIGHT = 65535,
                             MAX_DEPTH = 65535,
                             MAX_SPAWN_ROTATION = 255,
                             MAX_SPAWN_PITCH = 255,
                             MAX_BLOCK_LENGTH = 257,

                             MIN_WIDTH = 16,
                             MIN_HEIGHT = 16,
                             MIN_DEPTH = 16,
                             MIN_SPAWN_ROTATION = 0,
                             MIN_SPAWN_PITCH = 0,
                             MIN_BLOCK_LENGTH = 2,

                             MAX_METADATA_SIZE = 65535,
                             MAX_BLOCK_DATA_SIZE = Integer.MAX_VALUE,
                             MIN_BLOCK_DATA_SIZE = MIN_WIDTH * MIN_HEIGHT * MIN_DEPTH;

    private static final byte SPECIAL_BIT =  (byte)0x80,
                              SOLID_BIT =    (byte)0x40,
                              PHYSICS_BIT =  (byte)0x20,
                              MESSAGE_BIT =  (byte)0x10,
                              PORTAL_BIT =   (byte)0x08,
                              SCRIPTED_BIT = (byte)0x04;

    private static final long[] SUPPORTED_VERSIONS = new long[]{
        0xAA000001L
    };

    private final long version;
    private final int width, height, depth;
    private volatile int spawnWidth, spawnHeight, spawnDepth;
    private volatile short spawnRotation, spawnPitch;
    private final Map<String, byte[]> metadata;
    protected final byte[][] blockData;
    protected final short blockLength;

    public RUMMinecraftMap(long version,
                  int width, int height, int depth,
                  int spawnWidth, int spawnHeight, int spawnDepth,
                  int spawnRotation, int spawnPitch,
                  Map<String, byte[]> metadata,
                  byte[][] blockData,
                  int blockLength) throws InvalidMapException {
        if(!isVersionSupported(version))
            throw new InvalidMapException("Unsupported version");

        if(width > MAX_WIDTH || width < MIN_WIDTH)
            throw new InvalidMapException("Invalid width");

        if(height > MAX_HEIGHT || height < MIN_HEIGHT)
            throw new InvalidMapException("Invalid height");

        if(depth > MAX_DEPTH || depth < MIN_DEPTH)
            throw new InvalidMapException("Invalid depth");

        if(spawnWidth > MAX_WIDTH || spawnWidth < MIN_WIDTH)
            throw new InvalidMapException("Invalid spawn width");

        if(spawnHeight > MAX_HEIGHT || spawnHeight < MIN_HEIGHT)
            throw new InvalidMapException("Invalid spawn height");

        if(spawnDepth > MAX_DEPTH || spawnDepth < MIN_DEPTH)
            throw new InvalidMapException("Invalid spawn depth");

        if(spawnRotation > MAX_SPAWN_ROTATION || spawnRotation < MIN_SPAWN_ROTATION)
            throw new InvalidMapException("Invalid spawn rotation");

        if(spawnPitch > MAX_SPAWN_PITCH || spawnPitch < MIN_SPAWN_PITCH)
            throw new InvalidMapException("Invalid spawn pitch");

        if(metadata.size() > MAX_METADATA_SIZE)
            throw new InvalidMapException("Invalid metadata size");

        if(blockLength > MAX_BLOCK_LENGTH || blockLength < MIN_BLOCK_LENGTH)
            throw new InvalidMapException("Invalid block length");

        if(blockData.length < MIN_BLOCK_DATA_SIZE || blockData.length > MAX_BLOCK_DATA_SIZE)
            throw new InvalidMapException("Invalid block array size");

        if(blockData[0].length != blockLength)
            throw new InvalidMapException("blockData inner array length does not equal blockLength");

        this.version = version;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.spawnWidth = spawnWidth;
        this.spawnHeight = spawnHeight;
        this.spawnDepth = spawnDepth;
        this.spawnRotation = (short)spawnRotation;
        this.spawnPitch = (short)spawnPitch;
        this.metadata = metadata;
        this.blockData = blockData;
        this.blockLength = (short)blockLength;

        if(isPlayerOutOfBounds(spawnWidth, spawnHeight, spawnDepth))
                throw new InvalidMapException("Spawn out of bounds");
    }

    public RUMMinecraftMap(File file) throws IOException, MapFormatException, NotImplementedException {
        ExtendedDataInputStream in;
        FileInputStream fis;

        fis = new FileInputStream(file);
        in = new ExtendedDataInputStream(fis);
        try {
            version = in.readUnsignedInt();
            if(version == SUPPORTED_VERSIONS[0]) {
                GZIPInputStream gin;
                ExtendedDataInputStream gdin;
                BigInteger totalBlocks;

                gin = new GZIPInputStream(in);
                gdin = new ExtendedDataInputStream(gin);
                {
                    int metaDataLength;

                    metaDataLength = gdin.readLEUnsignedShort();
                    metadata = Collections.synchronizedMap(
                        new HashMap<String, byte[]>(metaDataLength)
                    );
                    for(int i = 0; i < metaDataLength;i++) {
                        String name;
                        byte[] payload;

                        {
                            byte[] nameData;
                            int read = 0, nameLength;

                            nameLength = gdin.readLEUnsignedShort();
                            nameData = new byte[nameLength];
                            while(read < nameLength) {
                                int nread;
                                nread = gdin.read(nameData, read, nameLength - read);
                                if(nread == -1)
                                    throw new MapFormatException("Map file incomplete");
                                read += nread;
                            }
                            name = new String(nameData);
                        }

                        {
                            int read = 0, payloadLength;

                            payloadLength = gdin.readLEUnsignedShort();
                            payload = new byte[payloadLength];
                            while(read < payloadLength) {
                                int nread;
                                nread = gdin.read(payload, read, payloadLength - read);
                                if(nread == -1)
                                    throw new MapFormatException("Map file incomplete");
                                read += nread;
                            }
                        }

                        metadata.put(name, payload);
                    }
                }

                width = gdin.readLEUnsignedShort();
                height = gdin.readLEUnsignedShort();
                depth = gdin.readLEUnsignedShort();

                spawnWidth = gdin.readLEUnsignedShort();
                spawnHeight = gdin.readLEUnsignedShort();
                spawnDepth = gdin.readLEUnsignedShort();

                spawnRotation = (short)gdin.readLEUnsignedByte();
                spawnPitch = (short)gdin.readLEUnsignedByte();

                blockLength = (short)(2 + gdin.readLEUnsignedByte());

                totalBlocks = BigInteger.valueOf(width)
                    .multiply(BigInteger.valueOf(height))
                    .multiply(BigInteger.valueOf(depth));

                if(totalBlocks.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1)
                    throw new MapFormatException("Width, height, and depth are too long");

                {
                    int read = 0, blocksRead = 0, intTotalBlocks;
                    BigInteger dataLength, currentDataLength;

                    intTotalBlocks = totalBlocks.intValue();

                    dataLength = gdin.readLEUnsignedBigInteger(8);
                    currentDataLength = totalBlocks.multiply(BigInteger.valueOf(blockLength));
                    
                    if(dataLength.compareTo(currentDataLength) != 0)
                        throw new MapFormatException("Block data array has incorrect size");

                    blockData = new byte[intTotalBlocks][blockLength];
                    while(blocksRead < intTotalBlocks) {
                            int nread;
                            nread = gdin.read(blockData[blocksRead], read, blockLength - read);
                            if(nread == -1)
                                throw new MapFormatException("Map file incomplete");
                            read += nread;
                            if(read == blockLength) {
                                read = 0;
                                blocksRead++;
                            }
                    }

                    if(gdin.read() != -1)
                        throw new MapFormatException("Garbage at end of map file");
                }
            } else {
                throw new MapFormatException("Unsupported file version");
            }
        } catch(EOFException e) {
            throw new MapFormatException("Map file incomplete", e);
        } finally {
            in.close();
        }
    }

    public RUMMinecraftMap(RUMMinecraftMap rumMap) throws InvalidMapException {
        this(
            rumMap.getVersion(),
            rumMap.getWidth(),
            rumMap.getHeight(),
            rumMap.getDepth(),
            rumMap.getSpawnWidth(),
            rumMap.getSpawnHeight(),
            rumMap.getSpawnDepth(),
            rumMap.getSpawnRotation(),
            rumMap.getSpawnPitch(),
            rumMap.getMetaDataMap(),
            rumMap.getExtendedBlocks(),
            rumMap.getExtendedBlockLength()
        );
    }

    protected void saveVersion1(OutputStream out) throws IOException, NotImplementedException {
        ExtendedDataOutputStream dos;
        GZIPOutputStream gos;

        gos = new GZIPOutputStream(out);
        try {
            dos = new ExtendedDataOutputStream(gos);
            try {
                dos.writeLEUnsignedShort(metadata.size());
                {
                    Collection<byte[]> payloads;
                    Collection<String> names;
                    Iterator<byte[]> payloadIterator;
                    Iterator<String> nameIterator;

                    payloads = metadata.values();
                    names = metadata.keySet();
                    payloadIterator = payloads.iterator();
                    nameIterator = names.iterator();
                    while(nameIterator.hasNext() && payloadIterator.hasNext()) {
                        String name;
                        byte[] payload;

                        name = nameIterator.next();
                        payload = payloadIterator.next();

                        dos.writeLEUnsignedShort(name.length());
                        dos.writeBytes(name);
                        dos.writeLEUnsignedShort(payload.length);
                        dos.write(payload);
                    }
                }

                dos.writeLEUnsignedShort(width);
                dos.writeLEUnsignedShort(height);
                dos.writeLEUnsignedShort(depth);

                dos.writeLEUnsignedShort(spawnWidth);
                dos.writeLEUnsignedShort(spawnHeight);
                dos.writeLEUnsignedShort(spawnDepth);

                dos.writeLEUnsignedByte(spawnRotation);
                dos.writeLEUnsignedByte(spawnPitch);
                dos.writeLEUnsignedByte((short)(blockLength - 2));

                {
                    BigInteger blockDataLength;

                    blockDataLength = BigInteger.valueOf(blockData.length)
                            .multiply(BigInteger.valueOf(blockLength));

                    dos.writeLEUnsignedBigInteger(blockDataLength, 8);
                    for(int i = 0;i < blockData.length;i++) {
                        dos.write(blockData[i]);
                    }
                }
                dos.flush();
                gos.finish();
            } finally {
                dos.close();
            }
        } finally {
            gos.close();
        }
    }

    public void save(File file) throws IOException, NotImplementedException {
        OutputStream out;
        ExtendedDataOutputStream dos;

        
        if(isVersionSupported(version)) {
            out = new FileOutputStream(file);
            try {
                dos = new ExtendedDataOutputStream(out);
                try {
                    dos.writeUnsignedInt(version);
                    if(version == SUPPORTED_VERSIONS[0])
                        saveVersion1(out);
                } finally {
                    dos.close();
                }
            } finally {
                out.close();
            }
        } else {
            throw new IOException("Cannot save map, unsupported version");
        } 
    }

    public static boolean isVersionSupported(long version) {
        for(int i = 0;i < SUPPORTED_VERSIONS.length;i++) {
            if(SUPPORTED_VERSIONS[i] == version)
                return true;
        }
        return false;
    }

    protected int getBlockOffset(int width, int height, int depth) {
        return ((height * this.depth + depth) * this.width + width);
    }

    public boolean isOutOfBounds(int width, int height, int depth) {
        if(width < 0 || height < 0 || depth < 0
                || width >= this.width || height >= this.height || depth >= this.depth)
            return true;
        else
            return false;
    }

    public boolean isPlayerOutOfBounds(int width, int height, int depth) {
        return isOutOfBounds(width / 32, height / 32, depth / 32);
    }

    public byte[] getMetaData(String name) {
        return metadata.get(name);
    }

    public long getVersion() {
        return version;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public int getSpawnWidth() {
        return spawnWidth;
    }

    public int getSpawnHeight() {
        return spawnHeight;
    }

    public int getSpawnDepth() {
        return spawnDepth;
    }

    public short getSpawnRotation() {
        return spawnRotation;
    }

    public short getSpawnPitch() {
        return spawnPitch;
    }

    public boolean isBlockSpecial(int width, int height, int depth) {
        int offset;
        boolean special;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        special = ((blockData[offset][1] & SPECIAL_BIT) != 0);
        return special;
    }

    public boolean isBlockSolid(int width, int height, int depth) {
        int offset;
        boolean solid;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        solid = ((blockData[offset][1] & SOLID_BIT) != 0);
        return solid;
    }

    public boolean isBlockPhysics(int width, int height, int depth) {
        int offset;
        boolean physics;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        physics = ((blockData[offset][1] & PHYSICS_BIT) != 0);
        return physics;
    }

    public boolean isBlockMessage(int width, int height, int depth) {
        int offset;
        boolean message;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        message = ((blockData[offset][1] & MESSAGE_BIT) != 0);
        return message;
    }

    public boolean isBlockPortal(int width, int height, int depth) {
        int offset;
        boolean portal;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        portal = ((blockData[offset][1] & PORTAL_BIT) != 0);
        return portal;
    }

    public boolean isBlockScripted(int width, int height, int depth) {
        int offset;
        boolean scripted;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        scripted = ((blockData[offset][1] & SCRIPTED_BIT) != 0);
        return scripted;
    }

    public byte getBlock(int width, int height, int depth) {
        int offset;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);

        return blockData[offset][0];
    }

    public void setSpawn(int spawnWidth, int spawnHeight, int spawnDepth,
                         int spawnRotation, int spawnPitch) {
        if(isPlayerOutOfBounds(spawnWidth, spawnHeight, spawnDepth))
            throw new IndexOutOfBoundsException("Attempting to set spawn outside map boundries");

        if(spawnRotation > MAX_SPAWN_ROTATION && spawnRotation < MIN_SPAWN_ROTATION)
            throw new RuntimeException("Attempting to set invalid spawn rotation");

        if(spawnPitch > MAX_SPAWN_PITCH && spawnPitch < MIN_SPAWN_PITCH)
            throw new RuntimeException("Attempting to set invalid spawn pitch");

        this.spawnWidth = spawnWidth;
        this.spawnHeight = spawnHeight;
        this.spawnDepth = spawnDepth;
        this.spawnRotation = (short)spawnRotation;
        this.spawnPitch = (short)spawnPitch;
    }

    public void setBlock(int width, int height, int depth, byte value,
                         boolean special,
                         boolean solid,
                         boolean physics,
                         boolean message,
                         boolean portal,
                         boolean scripted) {
        int offset;
        byte extendedData = 0;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        blockData[offset][0] = value;

        if(special)
            extendedData |= SPECIAL_BIT;
        if(solid)
            extendedData |= SOLID_BIT;
        if(physics)
            extendedData |= PHYSICS_BIT;
        if(portal)
            extendedData |= PORTAL_BIT;
        if(message)
            extendedData |= MESSAGE_BIT;
        if(scripted)
            extendedData |= SCRIPTED_BIT;
        blockData[offset][1] = extendedData;
        for(int i = 2;i < blockLength;i++)
            blockData[offset][i] = 0;
    }

    protected void setBlockExtendable(int width, int height, int depth, byte value,
                                      boolean special,
                                      boolean solid,
                                      boolean physics,
                                      boolean message,
                                      boolean portal,
                                      boolean scripted) {
        int offset;
        byte extendedData = 0;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        blockData[offset][0] = value;

        if(special)
            extendedData |= SPECIAL_BIT;
        if(solid)
            extendedData |= SOLID_BIT;
        if(physics)
            extendedData |= PHYSICS_BIT;
        if(portal)
            extendedData |= PORTAL_BIT;
        if(message)
            extendedData |= MESSAGE_BIT;
        if(scripted)
            extendedData |= SCRIPTED_BIT;
        blockData[offset][1] = extendedData;
    }

    public void setBlock(int width, int height, int depth, byte value) {
        int offset;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        blockData[offset][0] = value;
        for(int i = 1;i < blockLength;i++)
            blockData[offset][i] = 0;
    }

    public void setMetaData(String name, byte[] value) {
        metadata.put(name, value);
    }

    public byte[] getBlocks() {
        byte[] blocks;

        blocks = new byte[blockData.length];
        for(int i = 0;i < blockData.length;i++)
            blocks[i] = blockData[i][0];
        return blocks;
    }

    public byte[][] getExtendedBlocks() {
        return Arrays.copyOf(blockData, blockData.length);
    }

    public short getExtendedBlockLength() {
        return blockLength;
    }

    public Map<String, byte[]> getMetaDataMap() {
        return Collections.synchronizedMap(
            new HashMap<String, byte[]>(metadata)
        );
    }

    @Override
    public RUMMinecraftMap clone() {
        try {
            return new RUMMinecraftMap(this);
        } catch(InvalidMapException e) {
            throw new RuntimeException("Could not clone map", e);
        }
    }
}
