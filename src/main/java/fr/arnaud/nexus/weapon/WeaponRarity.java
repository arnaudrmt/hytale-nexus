package fr.arnaud.nexus.weapon;

/**
 * Weapon rarity. Controls enchantment slot count and base stat scaling.
 *
 * GDD refs (§ Fiche Armes / Statistiques de Base & Rareté):
 *   Rarity influences: number of enchantment slots and stat values at base.
 *
 *   Slot counts are a "binary choice at loot" per the GDD — players pick
 *   from a set of 1–3 slots when a weapon drops.
 */
public enum WeaponRarity {

    COMMON(1, 1.0f),
    UNCOMMON(2, 1.25f),
    RARE(2, 1.5f),
    EPIC(3, 1.75f),
    LEGENDARY(3, 1.0f);

    /** Maximum number of enchantment slots this rarity can have. */
    private final int maxEnchantSlots;

    /** Base stat multiplier applied to raw weapon stats at generation. */
    private final float statMultiplier;

    WeaponRarity(int maxEnchantSlots, float statMultiplier) {
        this.maxEnchantSlots = maxEnchantSlots;
        this.statMultiplier = statMultiplier;
    }

    public int getMaxEnchantSlots()  { return maxEnchantSlots; }
    public float getStatMultiplier() { return statMultiplier; }
}
