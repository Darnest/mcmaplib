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
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import mcmaplib.util.ExtendedDataInputStream;
import mcmaplib.util.ExtendedDataOutputStream;

public class RUMMinecraftMap extends MinecraftMap implements Cloneable, Serializable {
    private static final int MAX_BLOCK_LENGTH = 257,

                             MIN_BLOCK_LENGTH = 2,

                             MAX_METADATA_SIZE = 65535;

    private static final byte SPECIAL_BIT =  (byte)0x80,
                              SOLID_BIT =    (byte)0x40,
                              PHYSICS_BIT =  (byte)0x20,
                              MESSAGE_BIT =  (byte)0x10,
                              PORTAL_BIT =   (byte)0x08,
                              SCRIPTED_BIT = (byte)0x04;

    public static final long[] SUPPORTED_VERSIONS = new long[]{
        0xAA000001L
    };
    public static final long CURRENT_VERSION = SUPPORTED_VERSIONS[0];

    private final int width, height, depth;
    private volatile int spawnWidth, spawnHeight, spawnDepth;
    private volatile short spawnRotation, spawnPitch;
    private final Map<String, byte[]> metadata;
    protected final byte[][] blockData;
    protected final short blockLength;

    public RUMMinecraftMap(int width, int height, int depth,
                  int spawnWidth, int spawnHeight, int spawnDepth,
                  int spawnRotation, int spawnPitch,
                  Map<String, byte[]> metadata,
                  byte[][] blockData,
                  int blockLength) throws InvalidMapException {
        if(width > MAX_WIDTH || width < MIN_WIDTH)
            throw new InvalidMapException("Invalid width");

        if(height > MAX_HEIGHT || height < MIN_HEIGHT)
            throw new InvalidMapException("Invalid height");

        if(depth > MAX_DEPTH || depth < MIN_DEPTH)
            throw new InvalidMapException("Invalid depth");

        if(spawnWidth > MAX_SPAWN_WIDTH || spawnWidth < MIN_SPAWN_WIDTH)
            throw new InvalidMapException("Invalid spawn width");

        if(spawnHeight > MAX_SPAWN_HEIGHT || spawnHeight < MIN_SPAWN_HEIGHT)
            throw new InvalidMapException("Invalid spawn height");

        if(spawnDepth > MAX_SPAWN_DEPTH || spawnDepth < MIN_SPAWN_DEPTH)
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

    public RUMMinecraftMap(RUMMinecraftMap rumMap) throws InvalidMapException {
        this(
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

    private static Map<String, byte[]> getDefaultMetadata() {
        Map<String, byte[]> metadata;

        metadata = Collections.synchronizedMap(
            new HashMap<String, byte[]>()
        );

        metadata.put("_origin", "mcmaplib".getBytes());

        return metadata;
    }

    private static byte[][] extendBlocks(byte[] blocks, int blockLength) {
        byte[][] extendedBlocks;

        extendedBlocks = new byte[blocks.length][blockLength];
        for(int i = 0;i < blocks.length;i++) {
            extendedBlocks[i][0] = blocks[i];
        }
        return extendedBlocks;
    }

    public RUMMinecraftMap(MinecraftMap map) throws InvalidMapException {
        this(
            map.getWidth(),
            map.getHeight(),
            map.getDepth(),
            map.getSpawnWidth(),
            map.getSpawnHeight(),
            map.getSpawnDepth(),
            map.getSpawnRotation(),
            map.getSpawnPitch(),
            getDefaultMetadata(),
            extendBlocks(map.getBlocks(), 2),
            2
        );
    }

    public RUMMinecraftMap(MCSharpMinecraftMap mcSharpMap) throws InvalidMapException {
        this(
            mcSharpMap.getWidth(),
            mcSharpMap.getHeight(),
            mcSharpMap.getDepth(),
            mcSharpMap.getSpawnWidth(),
            mcSharpMap.getSpawnHeight(),
            mcSharpMap.getSpawnDepth(),
            mcSharpMap.getSpawnRotation(),
            mcSharpMap.getSpawnPitch(),
            getDefaultMetadata(),
            extendBlocks(mcSharpMap.getBlocks(), 2),
            2
        );
    }

    private static RUMMinecraftMap loadVersion1(InputStream in)
            throws IOException, EOFException, MapFormatException, NotImplementedException {
        RUMMinecraftMap map;
        int width, height, depth;
        int spawnWidth, spawnHeight, spawnDepth;
        short spawnRotation, spawnPitch;
        Map<String, byte[]> metadata;
        byte[][] blockData;
        short blockLength;
        GZIPInputStream gin;

        gin = new GZIPInputStream(in);
        try {
            ExtendedDataInputStream din;

            din = new ExtendedDataInputStream(gin);
            try {
                BigInteger totalBlocks;
                
                {
                    int metaDataLength;

                    metaDataLength = din.readLEUnsignedShort();
                    metadata = Collections.synchronizedMap(
                        new HashMap<String, byte[]>(metaDataLength)
                    );
                    for(int i = 0; i < metaDataLength;i++) {
                        String name;
                        byte[] payload;

                        {
                            byte[] nameData;
                            int read = 0, nameLength;

                            nameLength = din.readLEUnsignedShort();
                            nameData = new byte[nameLength];
                            while(read < nameLength) {
                                int nread;
                                nread = din.read(nameData, read, nameLength - read);
                                if(nread == -1)
                                    throw new EOFException();
                                read += nread;
                            }
                            name = new String(nameData);
                        }

                        {
                            int read = 0, payloadLength;

                            payloadLength = din.readLEUnsignedShort();
                            payload = new byte[payloadLength];
                            while(read < payloadLength) {
                                int nread;
                                nread = din.read(payload, read, payloadLength - read);
                                if(nread == -1)
                                    throw new EOFException();
                                read += nread;
                            }
                        }

                        metadata.put(name, payload);
                    }
                }

                width = din.readLEUnsignedShort();
                height = din.readLEUnsignedShort();
                depth = din.readLEUnsignedShort();

                spawnWidth = din.readLEUnsignedShort();
                spawnHeight = din.readLEUnsignedShort();
                spawnDepth = din.readLEUnsignedShort();

                spawnRotation = (short)din.readLEUnsignedByte();
                spawnPitch = (short)din.readLEUnsignedByte();

                blockLength = (short)(2 + din.readLEUnsignedByte());

                totalBlocks = BigInteger.valueOf(width)
                    .multiply(BigInteger.valueOf(height))
                    .multiply(BigInteger.valueOf(depth));

                if(totalBlocks.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1)
                    throw new MapFormatException("Width, height, and depth are too long");

                {
                    int read = 0, blocksRead = 0, intTotalBlocks;
                    BigInteger dataLength, currentDataLength;

                    intTotalBlocks = totalBlocks.intValue();

                    dataLength = din.readLEUnsignedBigInteger(8);
                    currentDataLength = totalBlocks.multiply(BigInteger.valueOf(blockLength));

                    if(dataLength.compareTo(currentDataLength) != 0)
                        throw new MapFormatException("Block data array has incorrect size");

                    blockData = new byte[intTotalBlocks][blockLength];
                    while(blocksRead < intTotalBlocks) {
                            int nread;
                            nread = din.read(blockData[blocksRead], read, blockLength - read);
                            if(nread == -1)
                                throw new EOFException();
                            read += nread;
                            if(read == blockLength) {
                                read = 0;
                                blocksRead++;
                            }
                    }

                    if(din.read() != -1)
                        throw new EOFException();
                }
            } finally {
                din.close();
            }
        } finally {
            gin.close();
        }

        try {
            map = new RUMMinecraftMap(
                width, height, depth,
                spawnWidth, spawnHeight, spawnDepth,
                spawnRotation, spawnPitch,
                metadata, blockData, blockLength
            );
        } catch(InvalidMapException e) {
            throw new MapFormatException(e);
        }

        return map;
    }

    public static RUMMinecraftMap load(File file)
            throws IOException, MapFormatException, NotImplementedException {
        RUMMinecraftMap map;
        FileInputStream fis;
        long version;

        fis = new FileInputStream(file);
        try {
            ExtendedDataInputStream din;
            
            din = new ExtendedDataInputStream(fis);
            try {
                version = din.readUnsignedInt();
                if(version == SUPPORTED_VERSIONS[0]) {
                    map = loadVersion1(din);
                } else {
                    throw new NotImplementedException("Unsupported file version");
                }
            } finally {
                din.close();
            }
        } catch(EOFException e) {
            throw new MapFormatException("Map file incomplete", e);
        } finally {
            fis.close();
        }
        return map;
    }

    private void saveVersion1(OutputStream out)
            throws IOException, NotImplementedException {
        GZIPOutputStream gos;

        gos = new GZIPOutputStream(out);
        try {
            ExtendedDataOutputStream dos;
            
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

    public void save(File file)
            throws IOException, NotImplementedException {
        save(file, CURRENT_VERSION);
    }

    public void save(File file, long version)
            throws IOException, NotImplementedException {
        OutputStream out;
        
        out = new FileOutputStream(file);
        try {
            ExtendedDataOutputStream dos;
            
            dos = new ExtendedDataOutputStream(out);
            try {
                dos.writeUnsignedInt(version);
                if(version == SUPPORTED_VERSIONS[0])
                    saveVersion1(out);
                else
                    throw new NotImplementedException("Cannot save map, unsupported version");
            } finally {
                dos.close();
            }
        } finally {
            out.close();
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
