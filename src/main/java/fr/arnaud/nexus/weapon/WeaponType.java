package fr.arnaud.nexus.weapon;

/**
 * Distinguishes the two weapon slot roles.
 *
 * GDD refs:
 *   MELEE  → Phase A of Switch Strike (triggers Reality Vision on Mini-Boss hit).
 *   RANGED → Phase C of Switch Strike (secondary ability auto-fires on exit).
 */
public enum WeaponType {
    MELEE,
    RANGED
}
