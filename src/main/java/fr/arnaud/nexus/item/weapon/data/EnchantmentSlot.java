package fr.arnaud.nexus.item.weapon.data;

import org.jetbrains.annotations.Nullable;

public record EnchantmentSlot(
    int slotIndex,
    String choiceA,
    String choiceB,
    @Nullable String chosen,
    int currentLevel
) {
    public static final int MAX_LEVEL = 5;

    public boolean isUnlocked() {
        return chosen != null;
    }

    public boolean isMaxLevel() {
        return currentLevel >= MAX_LEVEL;
    }

    public EnchantmentSlot withChoice(String enchantmentId) {
        return new EnchantmentSlot(slotIndex, choiceA, choiceB, enchantmentId, 1);
    }

    public EnchantmentSlot withLevel(int level) {
        return new EnchantmentSlot(slotIndex, choiceA, choiceB, chosen, level);
    }
}
