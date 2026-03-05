package fr.arnaud.nexus.weapon;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data model for a player weapon.
 *
 * GDD refs (§ Fiche Armes):
 *   - Base stats + rarity determine raw power at drop.
 *   - Refinement (Affinage) scales ALL base stats proportionally (×refinement modifier).
 *   - One elemental Core slot per weapon (null = empty).
 *   - 1–3 Enchantment slots (binary choice at loot time).
 *   - WeaponType distinguishes melee vs ranged for Switch Strike phase assignment.
 *
 * Designed for item-stack serialization: keep this as a plain data object with
 * no ECS dependencies; store it in WeaponSlotComponent.
 */
public final class WeaponData {

    // -------------------------------------------------------------------------
    // Refinement constants (GDD: levels 1–10)
    // -------------------------------------------------------------------------

    public static final int MAX_REFINEMENT = 10;

    /** Per-level stat multiplier increment: +10% per level. */
    private static final float REFINEMENT_STEP = 0.10f;

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    private final String id;           // Unique weapon type key (for i18n + registry)
    private final WeaponType type;
    private final WeaponRarity rarity;

    // -------------------------------------------------------------------------
    // Base stats (at refinement level 0)
    // -------------------------------------------------------------------------

    private final float baseDamage;
    private final int   baseFlowBonus;      // Extra Flow segments this weapon adds
    private final float baseMoveSpeed;      // Positive = faster, negative = slower
    private final float baseAttackSpeed;
    private final float baseResilience;

    // -------------------------------------------------------------------------
    // Mutable state
    // -------------------------------------------------------------------------

    private int refinementLevel;           // 0..MAX_REFINEMENT
    private @Nullable Element core;        // null = unslotted

    private final List<Enchantment> enchantments;

    // -------------------------------------------------------------------------
    // Builder-style constructor
    // -------------------------------------------------------------------------

    private WeaponData(Builder builder) {
        this.id              = builder.id;
        this.type            = builder.type;
        this.rarity          = builder.rarity;
        this.baseDamage      = builder.baseDamage;
        this.baseFlowBonus   = builder.baseFlowBonus;
        this.baseMoveSpeed   = builder.baseMoveSpeed;
        this.baseAttackSpeed = builder.baseAttackSpeed;
        this.baseResilience  = builder.baseResilience;
        this.refinementLevel = 0;
        this.core            = null;
        this.enchantments    = new ArrayList<>();

        int slotCount = Math.min(builder.enchantSlots, rarity.getMaxEnchantSlots());
        for (int i = 0; i < slotCount; i++) {
            enchantments.add(new Enchantment(id + ".enchant." + i));
        }
    }

    // -------------------------------------------------------------------------
    // Effective stat accessors (apply refinement multiplier + rarity scaling)
    // -------------------------------------------------------------------------

    private float refinementMultiplier() {
        return rarity.getStatMultiplier() * (1f + refinementLevel * REFINEMENT_STEP);
    }

    public float getEffectiveDamage()      { return baseDamage * refinementMultiplier(); }
    public float getMoveSpeedBonus()       { return baseMoveSpeed * refinementMultiplier(); }
    public float getAttackSpeedBonus()     { return baseAttackSpeed * refinementMultiplier(); }
    public float getResilience()           { return baseResilience * refinementMultiplier(); }
    public int   getFlowBonus()            { return baseFlowBonus; } // Segments are not scaled

    // -------------------------------------------------------------------------
    // Refinement (Dream Dust upgrade)
    // -------------------------------------------------------------------------

    /**
     * Increments refinement by one level if below cap.
     *
     * @return true if refinement succeeded.
     */
    public boolean refine() {
        if (refinementLevel >= MAX_REFINEMENT) return false;
        refinementLevel++;
        return true;
    }

    public int getRefinementLevel() { return refinementLevel; }

    /** Dream Dust cost for next refinement level. Increases with level. */
    public int getRefinementCost() {
        return refinementLevel >= MAX_REFINEMENT ? 0 : (refinementLevel + 1) * 15;
    }

    // -------------------------------------------------------------------------
    // Elemental Core
    // -------------------------------------------------------------------------

    public boolean hasCore()              { return core != null; }
    public @Nullable Element getCore()    { return core; }

    public void slotCore(@NonNullDecl Element element) {
        this.core = element;
    }

    // -------------------------------------------------------------------------
    // Enchantments
    // -------------------------------------------------------------------------

    public List<Enchantment> getEnchantments() {
        return Collections.unmodifiableList(enchantments);
    }

    /** Returns all enchantments currently active given {@code currentFlowSegments}. */
    public List<Enchantment> getActiveEnchantments(int currentFlowSegments) {
        List<Enchantment> active = new ArrayList<>();
        for (Enchantment e : enchantments) {
            if (e.isActiveAt(currentFlowSegments)) {
                active.add(e);
            }
        }
        return active;
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    public String getId()         { return id; }
    public WeaponType getType()   { return type; }
    public WeaponRarity getRarity() { return rarity; }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder(@NonNullDecl String id, @NonNullDecl WeaponType type,
                                  @NonNullDecl WeaponRarity rarity) {
        return new Builder(id, type, rarity);
    }

    public static final class Builder {
        private final String id;
        private final WeaponType type;
        private final WeaponRarity rarity;
        private float baseDamage      = 10f;
        private int   baseFlowBonus   = 0;
        private float baseMoveSpeed   = 0f;
        private float baseAttackSpeed = 0f;
        private float baseResilience  = 0f;
        private int   enchantSlots    = 1;

        private Builder(String id, WeaponType type, WeaponRarity rarity) {
            this.id = id; this.type = type; this.rarity = rarity;
        }

        public Builder damage(float v)       { baseDamage = v;      return this; }
        public Builder flowBonus(int v)      { baseFlowBonus = v;   return this; }
        public Builder moveSpeed(float v)    { baseMoveSpeed = v;   return this; }
        public Builder attackSpeed(float v)  { baseAttackSpeed = v; return this; }
        public Builder resilience(float v)   { baseResilience = v;  return this; }
        public Builder enchantSlots(int v)   { enchantSlots = v;    return this; }

        public WeaponData build() { return new WeaponData(this); }
    }
}
