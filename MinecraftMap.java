package mcmaplib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public abstract class MinecraftMap implements Cloneable, Serializable {
    protected static final int MAX_WIDTH = 65535,
                               MAX_HEIGHT = 65535,
                               MAX_DEPTH = 65535,
                               MAX_SPAWN_WIDTH = 65535,
                               MAX_SPAWN_HEIGHT = 65535,
                               MAX_SPAWN_DEPTH = 65535,
                               MAX_SPAWN_ROTATION = 255,
                               MAX_SPAWN_PITCH = 255,

                               MIN_WIDTH = 16,
                               MIN_HEIGHT = 16,
                               MIN_DEPTH = 16,
                               MIN_SPAWN_WIDTH = 0,
                               MIN_SPAWN_HEIGHT = 0,
                               MIN_SPAWN_DEPTH = 0,
                               MIN_SPAWN_ROTATION = 0,
                               MIN_SPAWN_PITCH = 0,

                               MAX_BLOCK_DATA_SIZE = Integer.MAX_VALUE,
                               MIN_BLOCK_DATA_SIZE = MIN_WIDTH * MIN_HEIGHT * MIN_DEPTH;
    
    public abstract void setBlock(int width, int height, int depth, byte type);
    public abstract byte getBlock(int width, int height, int depth);
    public abstract boolean isOutOfBounds(int width, int height, int depth);
    public abstract boolean isPlayerOutOfBounds(int width, int height, int depth);
    public abstract byte[] getBlocks();
    public abstract void setSpawn(int spawnWidth, int spawnHeight, int spawnDepth,
                         int spawnRotation, int spawnPitch);
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract int getDepth();
    public abstract int getSpawnWidth();
    public abstract int getSpawnHeight();
    public abstract int getSpawnDepth();
    public abstract short getSpawnRotation();
    public abstract short getSpawnPitch();

    public void save(File file) throws IOException, NotImplementedException {
        FileOutputStream fos;

        fos = new FileOutputStream(file);
        try {
            save(fos);
        } finally {
            fos.close();
        }
    }

    public void save(OutputStream out) throws IOException, NotImplementedException {
        throw new NotImplementedException("Saving not implemented for this map format");
    }
    
    @Override
    public abstract MinecraftMap clone();
}