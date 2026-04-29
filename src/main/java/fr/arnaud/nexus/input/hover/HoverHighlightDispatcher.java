package fr.arnaud.nexus.input.hover;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Sends the appropriate highlight packet for a hovered block or entity.
 */
public final class HoverHighlightDispatcher {

    private static final String BLOCK_HIGHLIGHT_PARTICLE_ID = "Block_Top_Glow";
    private static final String ENTITY_HIGHLIGHT_PARTICLE_ID = "Block_Top_Glow";

    private static final float BLOCK_HIGHLIGHT_SCALE = 1.0f;
    private static final Color BLOCK_HIGHLIGHT_COLOR = new Color((byte) 255, (byte) 220, (byte) 80);

    private HoverHighlightDispatcher() {
    }

    public static void highlightBlock(
        @NonNullDecl Vector3i block,
        @NonNullDecl PlayerRef playerRef
    ) {

        Position center = new Position(
            block.getX() + 0.5,
            block.getY() + 1.5,
            block.getZ() + 0.5
        );

        Direction direction = new Direction(
            0,
            360,
            0
        );

        SpawnParticleSystem packet = new SpawnParticleSystem(
            BLOCK_HIGHLIGHT_PARTICLE_ID,
            center,
            direction,
            BLOCK_HIGHLIGHT_SCALE,
            BLOCK_HIGHLIGHT_COLOR
        );

        playerRef.getPacketHandler().writeNoCache(packet);
    }

    public static void highlightEntity(
        @NonNullDecl Entity entity,
        @NonNullDecl PlayerRef playerRef,
        @NonNullDecl Ref<EntityStore> playerEntityRef,
        @NonNullDecl Store<EntityStore> store
    ) {
        int entityId = entity.getNetworkId();

        ModelParticle glow = buildEntityGlowParticle();

        SpawnModelParticles packet = new SpawnModelParticles(
            entityId,
            new ModelParticle[]{glow}
        );

        playerRef.getPacketHandler().writeNoCache(packet);
    }

    private static ModelParticle buildEntityGlowParticle() {
        ModelParticle particle = new ModelParticle();
        particle.systemId = ENTITY_HIGHLIGHT_PARTICLE_ID;
        particle.scale = 1.0f;
        particle.targetEntityPart = EntityPart.Self;
        return particle;
    }
}
