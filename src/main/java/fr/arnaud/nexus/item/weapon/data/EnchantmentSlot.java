package fr.arnaud.nexus.item.weapon.data;

import org.jetbrains.annotations.Nullable;

public record EnchantmentSlot(
    int slotIndex,
    String choiceA,
    String choiceB,
    @Nullable String chosen,
    @Nullable String chosenBase,
    int currentLevel
) {
    public boolean isUnlocked() {
        return chosen != null;
    }

    public int getMaxLevel() {
        return 3;
    }

    public boolean isMaxLevel() {
        return currentLevel >= getMaxLevel();
    }

    public String resolveActiveEnchantId() {
        if (chosenBase == null) return null;
        return chosenBase + "_" + currentLevel;
    }

    public EnchantmentSlot withChoice(String baseFamily) {
        String level1Id = baseFamily + "_1";
        if (!level1Id.equals(choiceA + "_1")
            && !baseFamily.equals(choiceA)
            && !baseFamily.equals(choiceB)) {
            throw new IllegalArgumentException(
                baseFamily + " is not a valid choice for slot " + slotIndex);
        }
        return new EnchantmentSlot(slotIndex, choiceA, choiceB,
            baseFamily + "_1", baseFamily, 1);
    }

    public EnchantmentSlot withUpgradedLevel() {
        if (chosenBase == null) throw new IllegalStateException(
            "Cannot upgrade a locked slot.");
        int next = Math.min(currentLevel + 1, getMaxLevel());
        return new EnchantmentSlot(slotIndex, choiceA, choiceB,
            chosenBase + "_" + next, chosenBase, next);
    }
}
