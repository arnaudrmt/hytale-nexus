package fr.arnaud.nexus.weapon;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Represents a single enchantment slot on a weapon.
 *
 * GDD refs (§ Fiche Armes / Enchantements — Seuil de Flow):
 *   - Each slot has a level 0..7.
 *   - Activation requires currentFlowSegments >= level.
 *   - Levels 6-7 require expanded Flow capacity (weapons with +1/+2 Flow Bonus).
 *   - Upgraded via Dream Dust spend in the Weapon Nexus UI.
 *
 * Enchantments are PASSIVE effects; they activate automatically when
 * the Flow threshold is met (checked each combat tick).
 */
public final class Enchantment {

    /** Maximum enchantment level per slot. */
    public static final int MAX_LEVEL = 7;

    /** Minimum Flow segments required to activate Tier 2 (gated) enchants. */
    public static final int HIGH_TIER_FLOW_THRESHOLD = 6;

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Unique string key used for i18n lookup and serialization. */
    private final String id;

    // -------------------------------------------------------------------------
    // State (mutable — upgraded via Refinement)
    // -------------------------------------------------------------------------

    private int level;   // 0 = not yet purchased; 1..7 = active tiers

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public Enchantment(@NonNullDecl String id) {
        this.id    = id;
        this.level = 0;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public @NonNullDecl String getId() { return id; }
    public int getLevel()          { return level; }
    public boolean isPurchased()   { return level > 0; }

    /**
     * Returns the minimum Flow segments required for this enchantment's current
     * level to be considered active.
     *
     * GDD: "Activation requires segments = enchantment level."
     */
    public int getFlowThreshold()  { return level; }

    public boolean isActiveAt(int currentSegments) {
        return level > 0 && currentSegments >= level;
    }

    // -------------------------------------------------------------------------
    // Mutation (Dream Dust upgrade)
    // -------------------------------------------------------------------------

    /**
     * Attempts to upgrade the enchantment by one level.
     *
     * @return true if upgrade succeeded; false if already at MAX_LEVEL.
     */
    public boolean upgrade() {
        if (level >= MAX_LEVEL) return false;
        level++;
        return true;
    }

    /**
     * Dream Dust cost to upgrade to the next level.
     * Cost scales quadratically to make high-tier upgrades meaningful investments.
     */
    public int getUpgradeCost() {
        if (level >= MAX_LEVEL) return 0;
        return (level + 1) * (level + 1) * 10;
    }
}
