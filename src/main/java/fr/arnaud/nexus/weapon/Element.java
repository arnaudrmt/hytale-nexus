package fr.arnaud.nexus.weapon;

/**
 * Elemental affinities for weapons, mobs, and Cores.
 *
 * GDD refs (§ Fiche Armes / Système de Noyaux & § Switch Strike / Résistance Check):
 *   - Weakness match  → increased damage + extended weak-point window.
 *   - Resistance match → reduced damage + ability disabled.
 *   - Neutral         → standard damage.
 *
 * NONE represents an unslotted weapon core (no elemental interactions).
 */
public enum Element {
    NONE,
    FIRE,
    ICE,
    VOID;

    /**
     * Determines the interaction result when this element is used
     * against a mob with the given {@code mobStrength} and {@code mobWeakness}.
     */
    public ElementInteraction checkAgainst(Element mobWeakness, Element mobStrength) {
        if (this == NONE) return ElementInteraction.NEUTRAL;
        if (this == mobWeakness) return ElementInteraction.WEAKNESS;
        if (this == mobStrength) return ElementInteraction.RESISTANCE;
        return ElementInteraction.NEUTRAL;
    }
}
