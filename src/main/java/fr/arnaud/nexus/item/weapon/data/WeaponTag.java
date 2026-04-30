package fr.arnaud.nexus.item.weapon.data;

public enum WeaponTag {
    MELEE,
    RANGED,
    ANY;

    public boolean isCompatibleWith(WeaponTag weaponTag) {
        return this == ANY || weaponTag == ANY || this == weaponTag;
    }
}
