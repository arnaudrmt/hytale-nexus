package fr.arnaud.nexus.ui.inventory;

public final class QualityMapper {

    public static final String COLOR_COMMON = "#9d9d9d";
    public static final String COLOR_UNCOMMON = "#1eff00";
    public static final String COLOR_RARE = "#0070dd";
    public static final String COLOR_EPIC = "#a335ee";
    public static final String COLOR_LEGENDARY = "#FFD700";
    public static final String COLOR_DEVELOPER = "#ff0000";

    public static final String SLOT_COMMON = "Inventory/Slots/SlotCommon.png";
    public static final String SLOT_UNCOMMON = "Inventory/Slots/SlotUncommon.png";
    public static final String SLOT_RARE = "Inventory/Slots/SlotRare.png";
    public static final String SLOT_EPIC = "Inventory/Slots/SlotEpic.png";
    public static final String SLOT_LEGENDARY = "Inventory/Slots/SlotLegendary.png";
    public static final String SLOT_DEVELOPER = "Inventory/Slots/SlotDeveloper.png";

    public static final String NAME_COMMON = "Common";
    public static final String NAME_UNCOMMON = "Uncommon";
    public static final String NAME_RARE = "Rare";
    public static final String NAME_EPIC = "Epic";
    public static final String NAME_LEGENDARY = "Legendary";
    public static final String NAME_DEVELOPER = "Developer";

    private QualityMapper() {
    }

    public static String toColor(int quality) {
        return switch (quality) {
            case 0 -> COLOR_COMMON;
            case 1 -> COLOR_UNCOMMON;
            case 2 -> COLOR_RARE;
            case 3 -> COLOR_EPIC;
            case 4 -> COLOR_LEGENDARY;
            default -> COLOR_DEVELOPER;
        };
    }

    public static String toSlotImage(int quality) {
        return switch (quality) {
            case 0 -> SLOT_COMMON;
            case 1 -> SLOT_UNCOMMON;
            case 2 -> SLOT_RARE;
            case 3 -> SLOT_EPIC;
            case 4 -> SLOT_LEGENDARY;
            default -> SLOT_DEVELOPER;
        };
    }

    public static String toName(int quality) {
        return switch (quality) {
            case 0 -> NAME_COMMON;
            case 1 -> NAME_UNCOMMON;
            case 2 -> NAME_RARE;
            case 3 -> NAME_EPIC;
            case 4 -> NAME_LEGENDARY;
            default -> NAME_DEVELOPER;
        };
    }

    public static int levelToQuality(int level) {
        return Math.max(0, (level - 1) / 10);
    }

    public static String levelToColor(int level) {
        return toColor(levelToQuality(level));
    }

    public static String levelToSlotImage(int level) {
        return toSlotImage(levelToQuality(level));
    }
}
