package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class PlayerOcclusionComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, PlayerOcclusionComponent> componentType;


    private static final int Y_OFFSET = 512;
    private static final int XZ_OFFSET = 2097152;
    private boolean destroyed = false;

    private final Long2IntOpenHashMap replacedBlocks = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap replacedRotations = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap replacedFillers = new Long2IntOpenHashMap();
    private final LongOpenHashSet barrierColumnPositions = new LongOpenHashSet();
    private final LongOpenHashSet blockUtilPackedPositions = new LongOpenHashSet();

    public PlayerOcclusionComponent() {
        replacedBlocks.defaultReturnValue(0);
        replacedRotations.defaultReturnValue(0);
        replacedFillers.defaultReturnValue(0);
    }

    public void putReplaced(int x, int y, int z, int originalBlockId, int rotation, int filler) {
        long key = pack(x, y, z);
        replacedBlocks.put(key, originalBlockId);
        replacedRotations.put(key, rotation);
        replacedFillers.put(key, filler);
        blockUtilPackedPositions.add(BlockUtil.pack(x, y, z));
    }

    public void removeReplaced(int x, int y, int z) {
        long key = pack(x, y, z);
        replacedBlocks.remove(key);
        replacedRotations.remove(key);
        replacedFillers.remove(key);
        barrierColumnPositions.remove(key);
        blockUtilPackedPositions.remove(BlockUtil.pack(x, y, z));
    }

    public boolean isReplaced(int x, int y, int z) {
        return replacedBlocks.containsKey(pack(x, y, z));
    }

    public int getOriginalBlockId(int x, int y, int z) {
        return replacedBlocks.get(pack(x, y, z));
    }

    public int getOriginalRotation(int x, int y, int z) {
        return replacedRotations.get(pack(x, y, z));
    }

    public int getOriginalFiller(int x, int y, int z) {
        return replacedFillers.get(pack(x, y, z));
    }

    public void markBarrierColumn(int x, int y, int z) {
        barrierColumnPositions.add(pack(x, y, z));
    }

    public boolean isBarrierColumn(int x, int y, int z) {
        return barrierColumnPositions.contains(pack(x, y, z));
    }

    public void markDestroyed() {
        this.destroyed = true;
    }

    public boolean isDestroyed() {
        return this.destroyed;
    }


    @NonNullDecl
    public LongOpenHashSet getBlockUtilPackedPositions() {
        return blockUtilPackedPositions;
    }

    public LongOpenHashSet getBarrierColumnPositions() {
        return barrierColumnPositions;
    }

    @NonNullDecl
    public Long2IntOpenHashMap getReplacedBlocks() {
        return replacedBlocks;
    }

    @NonNullDecl
    public LongOpenHashSet getReplacedPositions() {
        return new LongOpenHashSet(replacedBlocks.keySet());
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x + XZ_OFFSET) & 0x3FFFFFL) << 42
            | ((long) (y + Y_OFFSET) & 0x3FFL) << 32
            | ((long) (z + XZ_OFFSET) & 0xFFFFFFFFL);
    }

    @NonNullDecl
    public static int[] unpack(long packed) {
        int x = (int) ((packed >> 42) & 0x3FFFFFL) - XZ_OFFSET;
        int y = (int) ((packed >> 32) & 0x3FFL) - Y_OFFSET;
        int z = (int) (packed & 0xFFFFFFFFL) - XZ_OFFSET;
        return new int[]{x, y, z};
    }

    @NonNullDecl
    public static ComponentType<EntityStore, PlayerOcclusionComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("OcclusionComponent not yet registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, PlayerOcclusionComponent> type) {
        componentType = type;
    }

    @NonNullDecl
    @Override
    public PlayerOcclusionComponent clone() {
        PlayerOcclusionComponent copy = new PlayerOcclusionComponent();
        copy.replacedBlocks.putAll(this.replacedBlocks);
        copy.replacedRotations.putAll(this.replacedRotations);
        copy.replacedFillers.putAll(this.replacedFillers);
        copy.barrierColumnPositions.addAll(this.barrierColumnPositions);
        copy.blockUtilPackedPositions.addAll(this.blockUtilPackedPositions);
        return copy;
    }
}
