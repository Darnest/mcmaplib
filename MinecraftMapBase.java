package mcmaplib;

import java.util.Arrays;

public class MinecraftMapBase extends MinecraftMap {
    protected final int width, height, depth;
    protected volatile int spawnWidth, spawnHeight, spawnDepth;
    protected volatile short spawnRotation, spawnPitch;
    protected final byte[] blocks;

    public MinecraftMapBase(byte[] blocks,
                            int width, int height, int depth,
                            int spawnWidth, int spawnHeight, int spawnDepth,
                            int spawnRotation, int spawnPitch)
                                throws InvalidMapException {
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

        if(blocks.length < MIN_BLOCK_DATA_SIZE || blocks.length > MAX_BLOCK_DATA_SIZE)
            throw new InvalidMapException("Invalid block array size");

        this.width = width;
        this.height = height;
        this.depth = depth;
        this.spawnWidth = spawnWidth;
        this.spawnHeight = spawnHeight;
        this.spawnDepth = spawnDepth;
        this.spawnRotation = (short)spawnRotation;
        this.spawnPitch = (short)spawnPitch;
        this.blocks = blocks;

        if(isPlayerOutOfBounds(spawnWidth, spawnHeight, spawnDepth))
            throw new InvalidMapException("Spawn out of bounds");
    }

    public MinecraftMapBase(MinecraftMap map) throws InvalidMapException {
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

    public byte getBlock(int width, int height, int depth) {
        int offset;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);

        return blocks[offset];
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

    public void setBlock(int width, int height, int depth, byte value) {
        int offset;

        if(isOutOfBounds(width, height, depth))
            throw new IndexOutOfBoundsException("attempting to access block outside map boundries");

        offset = getBlockOffset(width, height, depth);
        blocks[offset] = value;
    }
    

    public byte[] getBlocks() {
        return Arrays.copyOf(blocks, blocks.length);
    }

    @Override
    public MinecraftMapBase clone() {
        try {
            return new MinecraftMapBase(this);
        } catch(InvalidMapException e) {
            throw new RuntimeException("Could not clone map", e);
        }
    }
}