package fr.arnaud.nexus.item.weapon.enchantment.impl;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class EnchantEffectUtil {

    private EnchantEffectUtil() {
    }

    public static void applyEffect(@Nonnull Ref<EntityStore> targetRef,
                                   @Nonnull CommandBuffer<EntityStore> cmd,
                                   @Nonnull String effectId,
                                   float duration) {
        EffectControllerComponent effects = cmd.getComponent(
            targetRef, EffectControllerComponent.getComponentType());
        if (effects == null) return;

        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
        if (effect == null) return;

        effects.addEffect(targetRef, effect, duration, OverlapBehavior.OVERWRITE, cmd);
    }
}
