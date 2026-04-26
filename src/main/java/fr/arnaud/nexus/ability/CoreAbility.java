package fr.arnaud.nexus.ability;

public enum CoreAbility {

    DASH(
        "dash",
        "Dash",
        "Hold Ctrl and click to dash towards your cursor.\nCosts 1 Stamina point."
    ),
    SWITCH_STRIKE(
        "switch_strike",
        "Switch Strike",
        "When your ability damages an enemy, switch your weapon at the moment of impact to trigger a powerful combo.\nCosts 50% of your Stamina."
    );

    private final String id;
    private final String displayName;
    private final String description;

    CoreAbility(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static CoreAbility fromId(String id) {
        if (id == null) return null;
        for (CoreAbility ability : values()) {
            if (ability.id.equals(id)) return ability;
        }
        return null;
    }
}
