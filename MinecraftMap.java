package mcmaplib;

import java.io.Serializable;

public abstract class MinecraftMap  implements Cloneable, Serializable {
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

    @Override
    public abstract MinecraftMap clone();
}