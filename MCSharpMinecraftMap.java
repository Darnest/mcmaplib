package mcmaplib;

import java.math.BigInteger;
import java.io.IOException;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import mcmaplib.util.ExtendedDataOutputStream;

public class MCSharpMinecraftMap extends MinecraftMapBase {
    public static final int[] SUPPORTED_VERSIONS = new int[]{
        1874
    };
    public static final int CURRENT_VERSION = SUPPORTED_VERSIONS[0];


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

    private static LevelPermission getRUMMapPermission(RUMMinecraftMap map, String name) {
        byte[] permissionData;
        short permissionCode;
        LevelPermission permission;

        permissionData = map.getMetaData(name);
        if(permissionData.length > 0) {
            permissionCode = permissionData[0];
            if(permissionCode < 0)
                permissionCode = (short)((- permissionCode) + Byte.MIN_VALUE);
            permission = LevelPermission.fromCode(permissionCode);
        } else
            permission = LevelPermission.NULL;
        return permission;
    }

    private static LevelPermission getRUMMapVisitPermission(RUMMinecraftMap rumMap) {
        return getRUMMapPermission(rumMap, "mcsharp_visitPermission");
    }

    private static LevelPermission getRUMMapBuildPermission(RUMMinecraftMap rumMap) {
        return getRUMMapPermission(rumMap, "mcsharp_buildPermission");
    }

    public MCSharpMinecraftMap(RUMMinecraftMap rumMap) throws InvalidMapException {
        this(
            rumMap.getBlocks(),
            rumMap.getWidth(),
            rumMap.getHeight(),
            rumMap.getDepth(),
            rumMap.getSpawnWidth(),
            rumMap.getSpawnHeight(),
            rumMap.getSpawnDepth(),
            rumMap.getSpawnRotation(),
            rumMap.getSpawnPitch(),
            getRUMMapVisitPermission(rumMap),
            getRUMMapBuildPermission(rumMap)
        );
    }

    private static MCSharpMinecraftMap loadVersion1(DataInputStream din)
            throws IOException, EOFException, MapFormatException, NotImplementedException {
        MCSharpMinecraftMap map;
        int width, height, depth, spawnWidth, spawnHeight, spawnDepth;
        short spawnRotation, spawnPitch;
        byte[] blocks;
        LevelPermission buildPermission, visitPermission;

        width = din.readUnsignedShort();
        height = din.readUnsignedShort();
        depth = din.readUnsignedShort();
        spawnWidth = din.readUnsignedShort();
        spawnHeight = din.readUnsignedShort();
        spawnDepth = din.readUnsignedShort();
        spawnPitch = (short)din.readUnsignedByte();
        spawnRotation = (short)din.readUnsignedByte();

        {
            short buildPermissionCode, visitPermissionCode;

            visitPermissionCode = (short)din.readUnsignedByte();
            buildPermissionCode = (short)din.readUnsignedByte();
            
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

                nread = din.read(blocks);
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

    public static MCSharpMinecraftMap load(File file)
            throws IOException, MapFormatException, NotImplementedException {
        MCSharpMinecraftMap map;
        FileInputStream fin;
        DataInputStream din;
        int version;

        fin = new FileInputStream(file);
        try {
            din = new DataInputStream(fin);
            try {
                version = din.readUnsignedShort();
                if(version == SUPPORTED_VERSIONS[0]) {
                    map = loadVersion1(din);
                } else {
                    throw new NotImplementedException("Map version unsupported");
                }
            } finally {
                din.close();
            }
        } catch(EOFException e) {
            throw new MapFormatException("Map file incomplete", e);
        } finally {
            fin.close();
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

    public void save(File file, int version) throws IOException, NotImplementedException {
        FileOutputStream fos;

        fos = new FileOutputStream(file);
        try {
            ExtendedDataOutputStream dos;
            
            dos = new ExtendedDataOutputStream(fos);
            try {
                if(version == SUPPORTED_VERSIONS[0]) {
                    dos.writeUnsignedShort(version);
                    saveVersion1(dos);
                } else
                    throw new NotImplementedException("Unknown file version");
            } finally {
                dos.close();
            }
        } finally {
            fos.close();
        }
    }

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