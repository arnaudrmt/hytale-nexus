package fr.arnaud.nexus.ability.core;

public enum CoreAbility {

    DASH(
        "dash",
        "nexus.ability.dash.name",
        "nexus.ability.dash.description",
        "nexus.ability.dash.tooltip"
    ),
    SWITCH_STRIKE(
        "switch_strike",
        "nexus.ability.switchStrike.name",
        "nexus.ability.switchStrike.description",
        "nexus.ability.switchStrike.tooltip"
    );

    private final String id;
    private final String displayNameKey;
    private final String descriptionKey;
    private final String tooltipKey;

    CoreAbility(String id, String displayName, String description, String tooltipKey) {
        this.id = id;
        this.displayNameKey = displayName;
        this.descriptionKey = description;
        this.tooltipKey = tooltipKey;
    }

    public String getId() {
        return id;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public String getTooltipKey() {
        return tooltipKey;
    }

    public static CoreAbility getAbilityFromId(String id) {
        if (id == null) return null;
        for (CoreAbility ability : values()) {
            if (ability.id.equals(id)) return ability;
        }
        return null;
    }
}
