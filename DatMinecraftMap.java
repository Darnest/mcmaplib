package mcmaplib;

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import mcmaplib.util.ExtendedDataInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamConstants;
import java.io.ObjectStreamField;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import mcmaplib.util.ExtendedDataOutputStream;

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
            throw new MapFormatException("Holds wrong java serialized object", e);
        } catch(ClassCastException e)  {
            throw new MapFormatException("Holds wrong java serialized object", e);
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
            throw new MapFormatException(e.getMessage(), e);
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
        } catch(EOFException e) {
            throw new MapFormatException("Map file incomplete", e);
        }
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
        LevelObjectOutputStream los;
        GZIPOutputStream gos;
        Level level;

        gos = new GZIPOutputStream(out);
        dos = new ExtendedDataOutputStream(gos);
        dos.writeUnsignedInt(MAGIC);
        dos.writeUnsignedByte(VERSION_2);

        los = new LevelObjectOutputStream(dos);
        level = new Level();
        level.width = width;
        level.height = height;
        level.depth = depth;
        level.blocks = blocks;
        los.writeObject(level);
        los.flush();
        dos.flush();
        gos.finish();
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
}

class LevelObjectInputStream extends ObjectInputStream {
    public LevelObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
                         throws IOException, ClassNotFoundException {
        String name;

        name = desc.getName();
        if(name.equals("com.mojang.minecraft.level.Level")) {
            return Level.class;
        } else
            return super.resolveClass(desc);
    }
}

class LevelObjectOutputStream extends ObjectOutputStream {
    public LevelObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc)
                         throws IOException {
        String name;
        ObjectStreamField[] fields;

        name = desc.getName();
        fields = desc.getFields();
        System.out.println("name: " + name);
        if(name.equals("mcmaplib.Level")) {
            System.out.println("special write");
            writeUTF("com.mojang.minecraft.level.Level");
            writeLong(desc.getSerialVersionUID());

            byte flags = 0;
            flags |= ObjectStreamConstants.SC_SERIALIZABLE;
            flags |= ObjectStreamConstants.SC_WRITE_METHOD;
            writeByte(flags);

            writeShort(fields.length);
            for (int i = 0; i < fields.length; i++) {
                ObjectStreamField f = fields[i];
                writeByte(f.getTypeCode());
                writeUTF(f.getName());
                if (!f.isPrimitive()) {
                    writeUTF(f.getTypeString());
                }
            }
        } else
            super.writeClassDescriptor(desc);
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