package fr.arnaud.nexus.item.weapon.data;

public enum WeaponRarity {

    COMMON(0, 1.00f),
    RARE(1, 1.25f),
    EPIC(2, 1.50f),
    LEGENDARY(3, 1.75f);

    private final int enchantmentSlots;
    private final float damageMultiplier;

    WeaponRarity(int enchantmentSlots, float damageMultiplier) {
        this.enchantmentSlots = enchantmentSlots;
        this.damageMultiplier = damageMultiplier;
    }

    public int getEnchantmentSlots() {
        return enchantmentSlots;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public static WeaponRarity fromBsonString(String value) {
        for (WeaponRarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(value)) {
                return rarity;
            }
        }
        return COMMON;
    }
}
