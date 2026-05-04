package fr.arnaud.nexus.util;

public final class QualityMapper {

    public static final String COLOR_COMMON = "#CBD2DC";
    public static final String COLOR_UNCOMMON = "#548E51";
    public static final String COLOR_RARE = "#3C64B2";
    public static final String COLOR_EPIC = "#813999";
    public static final String COLOR_LEGENDARY = "#FFD700";

    public static final String KEY_NAME_COMMON = "weapon.quality.common";
    public static final String KEY_NAME_UNCOMMON = "weapon.quality.uncommon";
    public static final String KEY_NAME_RARE = "weapon.quality.rare";
    public static final String KEY_NAME_EPIC = "weapon.quality.epic";
    public static final String KEY_NAME_LEGENDARY = "weapon.quality.legendary";

    public static final String SLOT_ID_COMMON = "#SlotCommonButton";
    public static final String SLOT_ID_UNCOMMON = "#SlotUncommonButton";
    public static final String SLOT_ID_RARE = "#SlotRareButton";
    public static final String SLOT_ID_EPIC = "#SlotEpicButton";
    public static final String SLOT_ID_LEGENDARY = "#SlotLegendaryButton";

    private QualityMapper() {
    }

    public static String toColor(int quality) {
        return getString(quality, COLOR_COMMON, COLOR_UNCOMMON, COLOR_RARE, COLOR_EPIC, COLOR_LEGENDARY);
    }

    private static String getString(int quality, String colorCommon, String colorUncommon, String colorRare, String colorEpic, String colorLegendary) {
        return switch (quality) {
            case 2 -> colorUncommon;
            case 3 -> colorRare;
            case 4 -> colorEpic;
            case 5 -> colorLegendary;
            default -> colorCommon;
        };
    }

    public static String toNameKey(int quality) {
        return getString(quality, KEY_NAME_COMMON, KEY_NAME_UNCOMMON, KEY_NAME_RARE, KEY_NAME_EPIC, KEY_NAME_LEGENDARY);
    }

    public static int levelToQuality(int level) {
        return Math.max(0, (level - 1) / 10);
    }

    public static String levelToColor(int level) {
        return toColor(levelToQuality(level));
    }

    public static String toSlotElementId(int quality) {
        return switch (quality) {
            case 2 -> SLOT_ID_UNCOMMON;
            case 3 -> SLOT_ID_RARE;
            case 4 -> SLOT_ID_EPIC;
            case 5 -> SLOT_ID_LEGENDARY;
            default -> SLOT_ID_COMMON;
        };
    }
}
