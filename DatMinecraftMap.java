package mcmaplib;

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import mcmaplib.util.ExtendedDataInputStream;
import java.io.InputStream;
import java.io.ObjectStreamClass;

public class DatMinecraftMap extends MinecraftMapBase {
    private static final long MAGIC = 0x271bb788;
    private static final short[] SUPPORTED_VERSIONS = new short[] {
        2
    };
    public static final short VERSION_2 = SUPPORTED_VERSIONS[0],
                              CURRENT_VERSION = VERSION_2;

    public DatMinecraftMap(byte[] blocks,
                           int width, int height, int depth,
                           int spawnWidth, int spawnHeight, int spawnDepth,
                           int spawnRotation, int spawnPitch)
                                throws InvalidMapException {
        super(
            blocks,
            width, height, depth,
            spawnWidth, spawnHeight, spawnDepth,
            spawnRotation, spawnPitch
        );
    }

    public DatMinecraftMap(MinecraftMap map) throws InvalidMapException {
        this(
            map.getBlocks(),
            map.getWidth(),
            map.getHeight(),
            map.getDepth(),
            map.getSpawnWidth(),
            map.getSpawnHeight(),
            map.getSpawnDepth(),
            map.getSpawnRotation(),
            map.getSpawnPitch()
        );
    }

    private static DatMinecraftMap loadVersion2(DataInputStream dis)
            throws IOException, NotImplementedException, MapFormatException {
        DatMinecraftMap map;
        LevelObjectInputStream ois;
        Level level;

        ois = new LevelObjectInputStream(dis);
        try {
           level = (Level)ois.readObject();
        } catch(ClassNotFoundException e) {
            throw new MapFormatException("Dat file corrupted", e);
        } catch(ClassCastException e)  {
            throw new MapFormatException("Dat file corrupted", e);
        }

        try {
            map = new DatMinecraftMap(
                level.blocks,
                level.width,
                level.height,
                level.depth,
                level.xSpawn,
                level.ySpawn,
                level.zSpawn,
                Math.abs(Math.round((level.rotSpawn * 255) % 255)),
                150
            );
        } catch(InvalidMapException e) {
            throw new MapFormatException("Dat file corrupted", e);
        }
        return map;
    }

    public static DatMinecraftMap load(File file)
            throws IOException, NotImplementedException, MapFormatException {
        FileInputStream fis;
        DatMinecraftMap map;

        fis = new FileInputStream(file);
        try {
            map = load(fis);
        } finally {
            fis.close();
        }
        return map;
    }

    public static DatMinecraftMap load(InputStream in)
            throws IOException, NotImplementedException, MapFormatException {
        DatMinecraftMap map;
        GZIPInputStream gis;

        gis = new GZIPInputStream(in);
        try {
            ExtendedDataInputStream dis;

            dis = new ExtendedDataInputStream(gis);
            try {
                long magic;
                short version;

                magic = dis.readUnsignedInt();
                if(magic != MAGIC)
                    throw new MapFormatException("Wrong magic constant");

                version = (short)dis.readUnsignedByte();
                if(version == VERSION_2)
                    map = loadVersion2(dis);
                else
                    throw new NotImplementedException("Unsupported version");
            } finally {
                dis.close();
            }
        } catch(EOFException e) {
            throw new MapFormatException("Map file incomplete", e);
        } finally {
            gis.close();
        }
        return map;
    }
}

class LevelObjectInputStream extends ObjectInputStream {
    public LevelObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
                         throws IOException, ClassNotFoundException {
        String name = desc.getName();
        if(name.equals("com.mojang.minecraft.level.Level")) {
            return Level.class;
        } else
            return super.resolveClass(desc);
    }
}

class Level implements Serializable {
    public static final long serialVersionUID = 0L;
    public int width;
    public int height;
    public int depth;
    public byte[] blocks;
    public String name;
    public String creator;
    public long createTime;
    public int xSpawn;
    public int ySpawn;
    public int zSpawn;
    public float rotSpawn;
    public boolean networkMode = false;
    public boolean creativeMode;
    public int waterLevel;
    public int skyColor;
    public int fogColor;
    public int cloudColor;
    public boolean growTrees = false;
}