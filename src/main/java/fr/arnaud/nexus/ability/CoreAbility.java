package fr.arnaud.nexus.ability;

/**
 * Registry of all equippable Core ability identifiers.
 * The string key is what gets persisted via {@link ActiveCoreComponent}.
 */
public enum CoreAbility {

    DASH("dash"),
    SWITCH_STRIKE("switch_strike");

    private final String id;

    CoreAbility(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * @return null if no match — callers must handle gracefully (empty slot is valid).
     */
    public static CoreAbility fromId(String id) {
        if (id == null) return null;
        for (CoreAbility ability : values()) {
            if (ability.id.equals(id)) return ability;
        }
        return null;
    }
}
