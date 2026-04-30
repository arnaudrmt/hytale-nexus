package fr.arnaud.nexus.tutorial;

public enum TutorialTriggerType {
    /**
     * Step advances only via a click in the UI.
     */
    MANUAL,
    /**
     * Advances when the player performs a weapon swap (Ability2).
     */
    WEAPON_SWAP,
    /**
     * Advances when the player opens the inventory.
     */
    INVENTORY_OPEN
}
