package fr.arnaud.nexus.weapon.data;

import javax.annotation.Nullable;

public record EnchantmentSlot(
    int slotIndex,
    String choiceA,
    String choiceB,
    @Nullable String chosen
) {
    public boolean isUnlocked() {
        return chosen != null;
    }

    public EnchantmentSlot withChoice(String enchantmentId) {
        if (!enchantmentId.equals(choiceA) && !enchantmentId.equals(choiceB)) {
            throw new IllegalArgumentException("Enchantment: " + enchantmentId + " is not a valid choice for slot " + slotIndex);
        }
        return new EnchantmentSlot(slotIndex, choiceA, choiceB, enchantmentId);
    }
}
