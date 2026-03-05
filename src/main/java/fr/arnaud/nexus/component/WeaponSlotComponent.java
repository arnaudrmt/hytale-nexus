package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.WeaponData;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nullable;

/**
 * Holds the player's two active weapon slots and exposes combined stat totals.
 *
 * GDD refs (§ Fiche Joueur / Statistiques Globales):
 *   Total Stats = [Melee Stats] + [Ranged Stats] + [Base Player]
 *
 * Any weapon equip / swap / refinement must call {@link #recalculate(FlowComponent)}
 * to propagate Flow capacity, generation rate, and resilience.
 */
public final class WeaponSlotComponent implements Component<EntityStore> {

    // -------------------------------------------------------------------------
    // ComponentType — injected once at plugin setup
    // -------------------------------------------------------------------------

    @Nullable
    private static ComponentType<EntityStore, WeaponSlotComponent> componentType;

    public static @NonNullDecl ComponentType<EntityStore, WeaponSlotComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("WeaponSlotComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, WeaponSlotComponent> type) {
        componentType = type;
    }

    // -------------------------------------------------------------------------
    // Base player stats (fixed, not affected by weapons)
    // -------------------------------------------------------------------------

    public static final float BASE_MOVE_SPEED    = 1.0f;
    public static final float BASE_ATTACK_SPEED  = 1.0f;
    public static final float BASE_RESILIENCE    = 0.0f;

    // -------------------------------------------------------------------------
    // Slots
    // -------------------------------------------------------------------------

    @Nullable private WeaponData meleeWeapon;
    @Nullable private WeaponData rangedWeapon;

    // -------------------------------------------------------------------------
    // Derived combined totals (recalculated on equip)
    // -------------------------------------------------------------------------

    private float totalDamage;
    private float totalMoveSpeed;
    private float totalAttackSpeed;
    private float totalResilience;
    private int   totalFlowBonus;      // Extra segments contributed by weapons

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public WeaponSlotComponent() {
        recalculate(null);
    }

    // -------------------------------------------------------------------------
    // Equip / swap
    // -------------------------------------------------------------------------

    public void equipMelee(@Nullable WeaponData weapon, @Nullable FlowComponent flow) {
        this.meleeWeapon = weapon;
        recalculate(flow);
    }

    public void equipRange(@Nullable WeaponData weapon, @Nullable FlowComponent flow) {
        this.rangedWeapon = weapon;
        recalculate(flow);
    }

    /**
     * Recomputes all combined totals and propagates Flow cap / retention to {@code flow}.
     * Called after any weapon change or refinement upgrade.
     */
    public void recalculate(@Nullable FlowComponent flow) {
        totalDamage      = 0f;
        totalMoveSpeed   = BASE_MOVE_SPEED;
        totalAttackSpeed = BASE_ATTACK_SPEED;
        totalResilience  = BASE_RESILIENCE;
        totalFlowBonus   = 0;

        if (meleeWeapon != null) {
            totalDamage      += meleeWeapon.getEffectiveDamage();
            totalMoveSpeed   += meleeWeapon.getMoveSpeedBonus();
            totalAttackSpeed += meleeWeapon.getAttackSpeedBonus();
            totalResilience  += meleeWeapon.getResilience();
            totalFlowBonus   += meleeWeapon.getFlowBonus();
        }
        if (rangedWeapon != null) {
            totalDamage      += rangedWeapon.getEffectiveDamage();
            totalMoveSpeed   += rangedWeapon.getMoveSpeedBonus();
            totalAttackSpeed += rangedWeapon.getAttackSpeedBonus();
            totalResilience  += rangedWeapon.getResilience();
            totalFlowBonus   += rangedWeapon.getFlowBonus();
        }

        if (flow != null) {
            flow.setMaxSegments(FlowComponent.BASE_MAX_SEGMENTS + totalFlowBonus);
            flow.setRetentionChance(resilienceToRetentionChance(totalResilience));
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public @Nullable WeaponData getMeleeWeapon()  { return meleeWeapon; }
    public @Nullable WeaponData getRangedWeapon() { return rangedWeapon; }
    public float getTotalDamage()      { return totalDamage; }
    public float getTotalMoveSpeed()   { return totalMoveSpeed; }
    public float getTotalAttackSpeed() { return totalAttackSpeed; }
    public float getTotalResilience()  { return totalResilience; }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts raw Resilience stat to a retention chance probability.
     * GDD example: high Resilience ≈ 30% chance to NOT lose a segment on hit.
     * Formula: chance = resilience / (resilience + 100)  [sigmoid-style soft cap]
     */
    private static float resilienceToRetentionChance(float resilience) {
        if (resilience <= 0) return 0f;
        return resilience / (resilience + 100f);
    }

    // -------------------------------------------------------------------------
    // ECS contract
    // -------------------------------------------------------------------------


    @NullableDecl
    @Override
    public WeaponSlotComponent clone() {
        WeaponSlotComponent copy = new WeaponSlotComponent();
        copy.meleeWeapon    = this.meleeWeapon;   // WeaponData is immutable after creation
        copy.rangedWeapon   = this.rangedWeapon;
        copy.totalDamage    = this.totalDamage;
        copy.totalMoveSpeed = this.totalMoveSpeed;
        copy.totalAttackSpeed = this.totalAttackSpeed;
        copy.totalResilience  = this.totalResilience;
        copy.totalFlowBonus   = this.totalFlowBonus;
        return copy;
    }
}
