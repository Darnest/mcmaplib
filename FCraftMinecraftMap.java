package mcmaplib;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import mcmaplib.util.ExtendedDataInputStream;
import mcmaplib.util.ExtendedDataOutputStream;

public class FCraftMinecraftMap extends MinecraftMapBase {
    private static final int[] SUPPORTED_VERSIONS = new int[] {
        0xFC000002
    };
    public static final int VERSION_2 = SUPPORTED_VERSIONS[0],
                            CURRENT_VERSION = VERSION_2;

    private final static Set<String> EXTENSIONS;
    private final static String NAME = "fCraft",
                                DESCRIPTION = "Map format for fCraft";

    static {
        Set<String> extensions = new HashSet<String>();
        extensions.add("fcm");
        EXTENSIONS = Collections.unmodifiableSet(extensions);
    }

    public static MapFormat FORMAT = new MapFormat() {
        public String getName() {
            return NAME;
        }

        public String getDescription() {
            return DESCRIPTION;
        }

        public Set<String> getExtensions() {
            return EXTENSIONS;
        }

        public FCraftMinecraftMap load(File file)
                throws IOException,
                       NotImplementedException,
                       MapFormatException,
                       FileNotFoundException {
            return FCraftMinecraftMap.load(file);
        }

        public FCraftMinecraftMap convert(MinecraftMap map)
                throws InvalidMapException {
            return new FCraftMinecraftMap(map);
        }
    };


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
        ExtendedDataInputStream dis;
        FCraftMinecraftMap map;
        int width, height, depth, spawnWidth, spawnHeight, spawnDepth;
        short spawnRotation, spawnPitch;
        byte[] blocks;
        Map<String, String> metadata;

        dis = new ExtendedDataInputStream(in);

        width = dis.readLEUnsignedShort();
        height = dis.readLEUnsignedShort();
        depth = dis.readLEUnsignedShort();
        spawnWidth = dis.readLEUnsignedShort();
        spawnHeight = dis.readLEUnsignedShort();
        spawnDepth = dis.readLEUnsignedShort();
        spawnRotation = (short)dis.readUnsignedByte();
        spawnPitch = (short)dis.readUnsignedByte();

        {
            int metadataSize;

            metadataSize = dis.readLEUnsignedShort();
            metadata = new HashMap<String, String>(metadataSize);
            for(int i = 0; i < metadataSize;i++) {
                String key, value;
                {
                    int keySize;
                    byte[] keydata;

                    keySize = dis.readLEUnsignedShort();
                    keydata = new byte[keySize];
                    dis.readFully(keydata);
                    key = new String(keydata);
                }
                {
                    int valueSize;
                    byte[] valuedata;

                    valueSize = dis.readLEUnsignedShort();
                    valuedata = new byte[valueSize];
                    dis.readFully(valuedata);
                    value = new String(valuedata);
                }
                metadata.put(key, value);
            }
        }
        {
            BigInteger totalBlocks;
            int intTotalBlocks;

            totalBlocks = BigInteger.valueOf(width)
                .multiply(BigInteger.valueOf(height))
                .multiply(BigInteger.valueOf(depth));

            if(totalBlocks.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1)
                throw new MapFormatException("Width, height, and depth are too large");

            intTotalBlocks = totalBlocks.intValue();
            blocks = new byte[intTotalBlocks];
        }

        {
            GZIPInputStream gis;
            DataInputStream gdis;

            gis = new GZIPInputStream(dis);
            gdis = new DataInputStream(gis);
            gdis.readFully(blocks);
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
            e.printStackTrace();
            throw new MapFormatException("Map data incomplete");
        } catch(IOException e) {
            e.printStackTrace();
            throw e;
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
        ExtendedDataInputStream dis;
        int version;

        dis = new ExtendedDataInputStream(in);
        version = (int)dis.readLEUnsignedInt();
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
        ExtendedDataOutputStream dos;

        dos = new ExtendedDataOutputStream(out);
        dos.writeLEUnsignedInt(VERSION_2);
        dos.writeLEUnsignedShort(width);
        dos.writeLEUnsignedShort(height);
        dos.writeLEUnsignedShort(depth);
        dos.writeLEUnsignedShort(spawnWidth);
        dos.writeLEUnsignedShort(spawnHeight);
        dos.writeLEUnsignedShort(spawnDepth);
        dos.writeByte(spawnRotation);
        dos.writeByte(spawnPitch);
        dos.writeLEUnsignedShort(metadata.size());

        {
            Iterator<String> keys, values;
            keys = metadata.keySet().iterator();
            values = metadata.values().iterator();

            while(keys.hasNext() && values.hasNext()) {
                String value, key;

                value = values.next();
                key = keys.next();
                dos.writeLEUnsignedShort(key.length());
                dos.writeBytes(key);
                dos.writeLEUnsignedShort(value.length());
                dos.writeBytes(value);
            }
        }
        {
            GZIPOutputStream gos;

            System.out.println(blocks.length);
            gos = new GZIPOutputStream(dos);
            gos.write(blocks);
            gos.finish();
            gos.flush();
        }
        dos.flush();
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