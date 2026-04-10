package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Ephemeral per-player component tracking blocks currently replaced with Air
 * by the {@code CameraOcclusionSystem}.
 *
 * <p>Stores both:
 * <ul>
 *   <li>A {@code Long2IntOpenHashMap} of packed position → original block ID
 *       for restoration.</li>
 *   <li>A {@code LongOpenHashSet} packed with {@link BlockUtil#pack} for direct
 *       use with {@code TargetUtil.getTargetBlockAvoidLocations}.</li>
 * </ul>
 */
public final class PlayerOcclusionComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, PlayerOcclusionComponent> componentType;

    @NonNullDecl
    public static ComponentType<EntityStore, PlayerOcclusionComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("OcclusionComponent not yet registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, PlayerOcclusionComponent> type) {
        componentType = type;
    }

    private static final int Y_OFFSET = 512;
    private static final int XZ_OFFSET = 2097152;

    /**
     * Maps our packed position → original block ID for restoration.
     */
    private final Long2IntOpenHashMap replacedBlocks = new Long2IntOpenHashMap();

    /**
     * The same positions packed with {@link BlockUtil#pack(int, int, int)} so
     * {@code TargetUtil.getTargetBlockAvoidLocations} can skip them directly.
     */
    private final LongOpenHashSet blockUtilPackedPositions = new LongOpenHashSet();

    public PlayerOcclusionComponent() {
        replacedBlocks.defaultReturnValue(0);
    }

    public void putReplaced(int x, int y, int z, int originalBlockId) {
        long key = pack(x, y, z);
        replacedBlocks.put(key, originalBlockId);
        blockUtilPackedPositions.add(BlockUtil.pack(x, y, z));
    }

    public void removeReplaced(int x, int y, int z) {
        replacedBlocks.remove(pack(x, y, z));
        blockUtilPackedPositions.remove(BlockUtil.pack(x, y, z));
    }

    public boolean isReplaced(int x, int y, int z) {
        return replacedBlocks.containsKey(pack(x, y, z));
    }

    public int getOriginalBlockId(int x, int y, int z) {
        return replacedBlocks.get(pack(x, y, z));
    }

    @NonNullDecl
    public Long2IntOpenHashMap getReplacedBlocks() {
        return replacedBlocks;
    }

    /**
     * Returns positions as packed longs using our own scheme (for iteration).
     */
    @NonNullDecl
    public LongOpenHashSet getReplacedPositions() {
        return new LongOpenHashSet(replacedBlocks.keySet());
    }

    /**
     * Returns positions packed with {@link BlockUtil#pack} — ready to pass
     * directly to {@code TargetUtil.getTargetBlockAvoidLocations}.
     */
    @NonNullDecl
    public LongOpenHashSet getBlockUtilPackedPositions() {
        return blockUtilPackedPositions;
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
    @Override
    public PlayerOcclusionComponent clone() {
        PlayerOcclusionComponent copy = new PlayerOcclusionComponent();
        copy.replacedBlocks.putAll(this.replacedBlocks);
        copy.blockUtilPackedPositions.addAll(this.blockUtilPackedPositions);
        return copy;
    }
}
