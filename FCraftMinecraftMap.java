package mcmaplib;

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.io.DataInputStream;
import java.util.Iterator;
import java.io.DataOutputStream;

public class FCraftMinecraftMap extends MinecraftMapBase {
    private static final int[] SUPPORTED_VERSIONS = new int[] {
        0xFC000002
    };
    public static final int VERSION_2 = SUPPORTED_VERSIONS[0],
                            CURRENT_VERSION = VERSION_2;

    public static boolean isVersionSupported(long version) {
        for(int i = 0;i < SUPPORTED_VERSIONS.length;i++) {
            if(SUPPORTED_VERSIONS[i] == version)
                return true;
        }
        return false;
    }

    private final Map<String, String> metadata;

    public FCraftMinecraftMap(byte[] blocks,
                           int width, int height, int depth,
                           int spawnWidth, int spawnHeight, int spawnDepth,
                           int spawnRotation, int spawnPitch,
                           Map<String, String> metadata)
                                throws InvalidMapException {
        super(
            blocks,
            width, height, depth,
            spawnWidth, spawnHeight, spawnDepth,
            spawnRotation, spawnPitch
        );
        this.metadata = metadata;
    }

    public FCraftMinecraftMap(MinecraftMap map) throws InvalidMapException {
        this(
            map.getBlocks(),
            map.getWidth(),
            map.getHeight(),
            map.getDepth(),
            map.getSpawnWidth(),
            map.getSpawnHeight(),
            map.getSpawnDepth(),
            map.getSpawnRotation(),
            map.getSpawnPitch(),
            new HashMap<String, String>()
        );
    }

    public FCraftMinecraftMap(FCraftMinecraftMap map) throws InvalidMapException {
        this(
            map.getBlocks(),
            map.getWidth(),
            map.getHeight(),
            map.getDepth(),
            map.getSpawnWidth(),
            map.getSpawnHeight(),
            map.getSpawnDepth(),
            map.getSpawnRotation(),
            map.getSpawnPitch(),
            map.getMetadataMap()
        );
    }

    private static FCraftMinecraftMap loadVersion2(InputStream in)
            throws IOException, NotImplementedException, MapFormatException {
        DataInputStream dis;
        FCraftMinecraftMap map;
        int width, height, depth, spawnWidth, spawnHeight, spawnDepth;
        short spawnRotation, spawnPitch;
        byte[] blocks;
        Map<String, String> metadata;

        dis = new DataInputStream(in);

        width = dis.readUnsignedShort();
        height = dis.readUnsignedShort();
        depth = dis.readUnsignedShort();
        spawnWidth = dis.readUnsignedShort();
        spawnHeight = dis.readUnsignedShort();
        spawnDepth = dis.readUnsignedShort();
        spawnRotation = (short)dis.readUnsignedByte();
        spawnPitch = (short)dis.readUnsignedByte();

        {
            int metadataSize;
            String key, value;

            metadataSize = dis.readUnsignedShort();
            metadata = new HashMap<String, String>(metadataSize);
            for(int i = 0; i < metadataSize;i++) {
                {
                    int keySize;
                    byte[] keydata;

                    keySize = dis.readUnsignedShort();
                    keydata = new byte[keySize];
                    dis.readFully(keydata);
                    key = new String(keydata);
                }
                {
                    int valueSize;
                    byte[] valuedata;

                    valueSize = dis.readUnsignedShort();
                    valuedata = new byte[valueSize];
                    dis.readFully(valuedata);
                    value = new String(valuedata);
                }
                metadata.put(key, value);
            }
        }
        {
            int blockSize;

            blockSize = dis.readInt();
            blocks = new byte[blockSize];
            dis.readFully(blocks);
        }

        try {
            map = new FCraftMinecraftMap(
                blocks,
                width, height, depth,
                spawnWidth, spawnHeight, spawnDepth,
                spawnRotation, spawnPitch,
                metadata
            );
        } catch(InvalidMapException e) {
            throw new MapFormatException(e);
        }
        return map;
    }

    public static FCraftMinecraftMap load(File file)
            throws IOException, NotImplementedException, MapFormatException {
        FileInputStream fis;
        FCraftMinecraftMap map;

        fis = new FileInputStream(file);
        try {
            map = load(fis);
        } catch(EOFException e) {
            throw new MapFormatException("Map data incomplete");
        } finally {
            fis.close();
        }
        return map;
    }

    public String getMetadata(String name) {
        return metadata.get(name);
    }

    public void setMetadata(String name, String value) {
        metadata.put(name, value);
    }

    public void removeMetadata(String name) {
        metadata.remove(name);
    }

    public Map<String, String> getMetadataMap() {
        return new HashMap<String, String>(metadata);
    }

    public static FCraftMinecraftMap load(InputStream in)
            throws IOException, NotImplementedException, MapFormatException {
        FCraftMinecraftMap map;
        DataInputStream dis;
        int version;

        dis = new DataInputStream(in);
        version = dis.readInt();
        if(version == VERSION_2)
            map = loadVersion2(dis);
        else
            throw new NotImplementedException("Unsupported version");
        
        return map;
    }

    public void save(File file, int version)
            throws IOException, NotImplementedException{
        FileOutputStream fos;

        fos = new FileOutputStream(file);
        try {
            save(fos, version);
        } finally {
            fos.close();
        }
    }

    public void saveVersion2(OutputStream out)
            throws IOException, NotImplementedException {
        DataOutputStream dos;

        dos = new DataOutputStream(out);
        dos.writeInt(VERSION_2);
        dos.writeShort(width);
        dos.writeShort(height);
        dos.writeShort(depth);
        dos.writeShort(spawnWidth);
        dos.writeShort(spawnHeight);
        dos.writeShort(spawnDepth);
        dos.writeByte(spawnRotation);
        dos.writeByte(spawnPitch);
        dos.writeShort(metadata.size());

        {
            Iterator<String> keys, values;
            keys = metadata.keySet().iterator();
            values = metadata.values().iterator();

            while(keys.hasNext() && values.hasNext()) {
                String value, key;

                value = values.next();
                key = keys.next();
                dos.writeShort(key.length());
                dos.writeBytes(key);
                dos.writeShort(value.length());
                dos.writeBytes(value);
            }
        }
        dos.writeInt(blocks.length);
        dos.write(blocks);
    }

    public void save(OutputStream out, int version)
            throws IOException, NotImplementedException{
        if(version == VERSION_2)
            saveVersion2(out);
        else
            throw new NotImplementedException("Unsupported version");
    }

    @Override
    public void save(File file)
            throws IOException, NotImplementedException{
        save(file, CURRENT_VERSION);
    }

    @Override
    public void save(OutputStream out)
            throws IOException, NotImplementedException{
        save(out, CURRENT_VERSION);
    }

    @Override
    public FCraftMinecraftMap clone() {
        try {
            return new FCraftMinecraftMap(this);
        } catch(InvalidMapException e) {
            throw new RuntimeException("Could not clone map", e);
        }
    }
}