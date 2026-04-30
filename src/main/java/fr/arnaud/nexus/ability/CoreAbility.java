package fr.arnaud.nexus.ability;

import com.hypixel.hytale.server.core.Message;

public enum CoreAbility {

    DASH(
        "dash",
        Message.translation("nexus.ability.dash.name"),
        Message.translation("nexus.ability.dash.description")
    ),
    SWITCH_STRIKE(
        "switch_strike",
        Message.translation("nexus.switch_strike.dash.name"),
        Message.translation("nexus.switch_strike.dash.description")
    );

    private final String id;
    private final Message displayName;
    private final Message description;

    CoreAbility(String id, Message displayName, Message description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public Message getDisplayName() {
        return displayName;
    }

    public Message getDescription() {
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
