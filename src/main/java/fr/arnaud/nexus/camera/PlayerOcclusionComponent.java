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
 *   <li>Parallel maps for original rotation index and filler so oriented
 *       blocks (stairs, logs, etc.) are restored in their correct state.</li>
 *   <li>A {@code LongOpenHashSet} of positions that sit inside the
 *       barrier-protection cylinder; these are replaced with Barrier blocks
 *       rather than Air so mobs cannot escape through the occlusion hole.</li>
 *   <li>A {@code LongOpenHashSet} packed with {@link BlockUtil#pack} for
 *       direct use with {@code TargetUtil.getTargetBlockAvoidLocations}.</li>
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
     * Maps our packed position → original rotation index for restoration.
     */
    private final Long2IntOpenHashMap replacedRotations = new Long2IntOpenHashMap();

    /**
     * Maps our packed position → original filler value for restoration.
     */
    private final Long2IntOpenHashMap replacedFillers = new Long2IntOpenHashMap();

    /**
     * Positions that were replaced with a Barrier block rather than Air.
     * These form the containment column that prevents mobs from escaping
     * through the occlusion hole around the player's feet.
     */
    private final LongOpenHashSet barrierColumnPositions = new LongOpenHashSet();

    /**
     * The same positions packed with {@link BlockUtil#pack(int, int, int)} so
     * {@code TargetUtil.getTargetBlockAvoidLocations} can skip them directly.
     */
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

    public LongOpenHashSet getBarrierColumnPositions() {
        return barrierColumnPositions;
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
