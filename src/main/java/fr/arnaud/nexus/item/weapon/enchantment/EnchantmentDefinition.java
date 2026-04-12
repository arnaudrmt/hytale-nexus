package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.item.weapon.data.WeaponTag;

import java.util.List;

public final class EnchantmentDefinition {

    private final String enchantmentId;
    private final String baseFamily;
    private final int level;
    private final WeaponTag compatibleTag;
    private final String behaviorId;
    private final float baseCost;
    private final float costCurve;
    private final List<EnchantmentLevelData> levelData;

    public EnchantmentDefinition(
        String enchantmentId,
        String baseFamily,
        int level,
        WeaponTag compatibleTag,
        String behaviorId,
        float baseCost,
        float costCurve,
        List<EnchantmentLevelData> levelData
    ) {
        this.enchantmentId = enchantmentId;
        this.baseFamily = baseFamily;
        this.level = level;
        this.compatibleTag = compatibleTag;
        this.behaviorId = behaviorId;
        this.baseCost = baseCost;
        this.costCurve = costCurve;
        this.levelData = List.copyOf(levelData);
    }

    public String getEnchantmentId() {
        return enchantmentId;
    }

    public String getBaseFamily() {
        return baseFamily;
    }

    public int getLevel() {
        return level;
    }

    public WeaponTag getCompatibleTag() {
        return compatibleTag;
    }

    public String getBehaviorId() {
        return behaviorId;
    }

    public float getBaseCost() {
        return baseCost;
    }

    public float getCostCurve() {
        return costCurve;
    }

    public List<EnchantmentLevelData> getLevelData() {
        return levelData;
    }

    public boolean isBehavioral() {
        return behaviorId != null && !behaviorId.isBlank();
    }

    public EnchantmentLevelData getDataForLevel(int targetLevel) {
        for (EnchantmentLevelData data : levelData) {
            if (data.getLevel() == targetLevel) return data;
        }
        return levelData.isEmpty() ? null : levelData.get(0);
    }

    /**
     * Cost to unlock at level 1 or upgrade to the next level.
     */
    public int computeCost(int targetLevel) {
        if (targetLevel <= 1) return Math.round(baseCost);
        float cost = baseCost;
        for (int i = 1; i < targetLevel; i++) {
            cost *= costCurve;
        }
        return Math.round(cost);
    }

    public enum ModifierType {ADD, MULTIPLY}

    public record StatModifierEntry(String statId, ModifierType type, float value) {
    }
}
