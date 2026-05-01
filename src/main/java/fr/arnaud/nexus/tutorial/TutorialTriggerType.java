package fr.arnaud.nexus.tutorial;

public enum TutorialTriggerType {
    MANUAL, // Step advances only via the NEXT button in the UI.
    WEAPON_SWAP, // Advances when the player performs a weapon swap (Ability2).
    INVENTORY_OPEN // Advances when the player opens the inventory.
}
