package mcmaplib;

import java.math.BigInteger;
import java.io.IOException;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import mcmaplib.util.ExtendedDataOutputStream;

public class MCSharpMinecraftMap extends MinecraftMapBase {
    private static final int[] SUPPORTED_VERSIONS = new int[] {
        1874
    };
    public static final int VERSION_1 = SUPPORTED_VERSIONS[0],
                            CURRENT_VERSION = VERSION_1;

    public static boolean isVersionSupported(long version) {
        for(int i = 0;i < SUPPORTED_VERSIONS.length;i++) {
            if(SUPPORTED_VERSIONS[i] == version)
                return true;
        }
        return false;
    }


    public static enum LevelPermission {
        NULL(0x99),
        GUEST(0x00),
        BUILDER(0x01),
        ADVBUILDER(0x02),
        MODERATOR(0x03),
        OPERATOR(0x04),
        ADMIN(0x05);

        public final short CODE;
        LevelPermission(int code) {
            this.CODE = (short)code;
        }

        public static LevelPermission fromCode(int code) {
            switch(code) {
                case 0x00:
                    return GUEST;
                case 0x01:
                    return BUILDER;
                case 0x02:
                    return ADVBUILDER;
                case 0x03:
                    return MODERATOR;
                case 0x04:
                    return OPERATOR;
                case 0x05:
                    return ADMIN;
                case 0x99:
                default:
                    return NULL;
            }
        }
    }

    private volatile LevelPermission visitPermission, buildPermission;
    
    public MCSharpMinecraftMap(byte[] blocks,
                               int width, int height, int depth,
                               int spawnWidth, int spawnHeight, int spawnDepth,
                               int spawnRotation, int spawnPitch,
                               LevelPermission visitPermission,
                               LevelPermission buildPermission)
                                   throws InvalidMapException {
        super(
            blocks,
            width, height, depth,
            spawnWidth, spawnHeight, spawnDepth,
            spawnRotation, spawnPitch
        );

        if(visitPermission == null)
            visitPermission = LevelPermission.NULL;

        if(buildPermission == null)
            buildPermission = LevelPermission.NULL;
        
        this.visitPermission = visitPermission;
        this.buildPermission = buildPermission;
    }

    public MCSharpMinecraftMap(MinecraftMap map) throws InvalidMapException {
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
            LevelPermission.NULL,
            LevelPermission.NULL
        );
    }

    public MCSharpMinecraftMap(MCSharpMinecraftMap mcSharpMap) throws InvalidMapException {
        this(
            mcSharpMap.getBlocks(),
            mcSharpMap.getWidth(),
            mcSharpMap.getHeight(),
            mcSharpMap.getDepth(),
            mcSharpMap.getSpawnWidth(),
            mcSharpMap.getSpawnHeight(),
            mcSharpMap.getSpawnDepth(),
            mcSharpMap.getSpawnRotation(),
            mcSharpMap.getSpawnPitch(),
            mcSharpMap.getVisitPermission(),
            mcSharpMap.getBuildPermission()
        );
    }

    private static MCSharpMinecraftMap loadVersion1(DataInputStream dis)
            throws IOException, EOFException, MapFormatException, NotImplementedException {
        MCSharpMinecraftMap map;
        int width, height, depth, spawnWidth, spawnHeight, spawnDepth;
        short spawnRotation, spawnPitch;
        byte[] blocks;
        LevelPermission buildPermission, visitPermission;

        width = dis.readUnsignedShort();
        height = dis.readUnsignedShort();
        depth = dis.readUnsignedShort();
        spawnWidth = dis.readUnsignedShort();
        spawnHeight = dis.readUnsignedShort();
        spawnDepth = dis.readUnsignedShort();
        spawnPitch = (short)dis.readUnsignedByte();
        spawnRotation = (short)dis.readUnsignedByte();

        {
            short buildPermissionCode, visitPermissionCode;

            visitPermissionCode = (short)dis.readUnsignedByte();
            buildPermissionCode = (short)dis.readUnsignedByte();
            
            visitPermission = LevelPermission.fromCode(visitPermissionCode);
            buildPermission = LevelPermission.fromCode(buildPermissionCode);
        }

        {
            BigInteger totalBlocks;
            int intTotalBlocks;

            totalBlocks = BigInteger.valueOf(width)
                .multiply(BigInteger.valueOf(height))
                .multiply(BigInteger.valueOf(depth));

            if(totalBlocks.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1)
                throw new MapFormatException("Width, height, and depth are too long");

            intTotalBlocks = totalBlocks.intValue();
            blocks = new byte[intTotalBlocks];
        }

        {
            int read = 0;

            while(read < blocks.length) {
                int nread;

                nread = dis.read(blocks);
                if(nread == -1)
                    throw new EOFException();
                read += nread;
            }
        }

        try {
            map = new MCSharpMinecraftMap(
                blocks,
                width, height, depth,
                spawnWidth, spawnHeight, spawnDepth,
                spawnRotation, spawnPitch,
                visitPermission, buildPermission
            );
        } catch(InvalidMapException e) {
            throw new MapFormatException(e);
        }
        return map;
    }

    public static MCSharpMinecraftMap load(InputStream in)
            throws IOException, MapFormatException, NotImplementedException {
        MCSharpMinecraftMap map;
        DataInputStream dis;
        int version;

        dis = new DataInputStream(in);
        version = dis.readUnsignedShort();
        if(version == SUPPORTED_VERSIONS[0]) {
            map = loadVersion1(dis);
        } else {
            throw new NotImplementedException("Map version unsupported");
        }
        return map;
    }

    public static MCSharpMinecraftMap load(File file)
            throws IOException, MapFormatException, NotImplementedException {
        MCSharpMinecraftMap map;
        FileInputStream fis;

        fis = new FileInputStream(file);
        try {
            map = load(fis);
        } catch(EOFException e) {
            throw new MapFormatException("Map file incomplete", e);
        } finally {
            fis.close();
        }
        return map;
    }

    private void saveVersion1(ExtendedDataOutputStream dos) throws IOException {
        dos.writeUnsignedShort(width);
        dos.writeUnsignedShort(height);
        dos.writeUnsignedShort(depth);

        dos.writeUnsignedShort(spawnWidth);
        dos.writeUnsignedShort(spawnHeight);
        dos.writeUnsignedShort(spawnDepth);

        dos.writeUnsignedByte(spawnPitch);
        dos.writeUnsignedByte(spawnRotation);

        dos.writeUnsignedByte(visitPermission.CODE);
        dos.writeUnsignedByte(buildPermission.CODE);

        dos.write(getBlocks());
    }

    public void save(OutputStream out, int version) throws IOException, NotImplementedException {
        ExtendedDataOutputStream dos;

        dos = new ExtendedDataOutputStream(out);
        if(version == SUPPORTED_VERSIONS[0]) {
            dos.writeUnsignedShort(version);
            saveVersion1(dos);
        } else
            throw new NotImplementedException("Unknown file version");
    }

    public void save(File file, int version) throws IOException, NotImplementedException {
        FileOutputStream fos;

        fos = new FileOutputStream(file);
        try {
            save(fos, version);
        } finally {
            fos.close();
        }
    }

    @Override
    public void save(OutputStream out) throws IOException {
        save(out, CURRENT_VERSION);
    }

    @Override
    public void save(File file) throws IOException {
        save(file, CURRENT_VERSION);
    }

    public static boolean isVersionSupported(int version) {
        for(int i = 0;i < SUPPORTED_VERSIONS.length;i++) {
            if(SUPPORTED_VERSIONS[i] == version)
                return true;
        }
        return false;
    }

    public LevelPermission getVisitPermission() {
        return visitPermission;
    }

    public LevelPermission getBuildPermission() {
        return buildPermission;
    }

    public void setVisitPermission(LevelPermission visitPermission) {
        this.visitPermission = visitPermission;
    }

    public void setBuildPermission(LevelPermission buildPermission) {
        this.buildPermission = buildPermission;
    }

    @Override
    public MCSharpMinecraftMap clone() {
        try {
            return new MCSharpMinecraftMap(this);
        } catch(InvalidMapException e) {
            throw new RuntimeException("Could not clone map", e);
        }
    }
}