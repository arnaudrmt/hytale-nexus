package fr.arnaud.nexus.weapon;

/**
 * Result of an elemental check between an attacking weapon and a target.
 *
 * GDD refs (§ Switch Strike / Règles de Résistance):
 *   WEAKNESS   → Critical damage + extended weak-point window.
 *   NEUTRAL    → Normal damage.
 *   RESISTANCE → Reduced damage + ability disabled + no Switch Strike bonus.
 */
public enum ElementInteraction {

    WEAKNESS(1.5f, true, true),
    NEUTRAL(1.0f, true, false),
    RESISTANCE(0.25f, false, false);

    /** Damage multiplier applied to the hit. */
    private final float damageMultiplier;

    /** Whether the weapon ability fires (false on resistance). */
    private final boolean abilityEnabled;

    /** Whether the weak-point window is extended beyond default duration. */
    private final boolean extendedWeakPointWindow;

    ElementInteraction(float damageMultiplier, boolean abilityEnabled, boolean extendedWeakPointWindow) {
        this.damageMultiplier = damageMultiplier;
        this.abilityEnabled = abilityEnabled;
        this.extendedWeakPointWindow = extendedWeakPointWindow;
    }

    public float getDamageMultiplier()       { return damageMultiplier; }
    public boolean isAbilityEnabled()        { return abilityEnabled; }
    public boolean isExtendedWeakPoint()     { return extendedWeakPointWindow; }
}
