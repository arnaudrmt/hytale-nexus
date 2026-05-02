package fr.arnaud.nexus.ui.inventory;

public final class QualityMapper {

    public static final String COLOR_COMMON = "#9d9d9d";
    public static final String COLOR_UNCOMMON = "#1eff00";
    public static final String COLOR_RARE = "#0070dd";
    public static final String COLOR_EPIC = "#a335ee";
    public static final String COLOR_LEGENDARY = "#FFD700";
    public static final String COLOR_SPECIAL = "#be253f";

    public static final String KEY_NAME_COMMON = "weapon.quality.common";
    public static final String KEY_NAME_UNCOMMON = "weapon.quality.uncommon";
    public static final String KEY_NAME_RARE = "weapon.quality.rare";
    public static final String KEY_NAME_EPIC = "weapon.quality.epic";
    public static final String KEY_NAME_LEGENDARY = "weapon.quality.legendary";
    public static final String KEY_NAME_SPECIAL = "weapon.quality.special";

    private QualityMapper() {
    }

    public static String toColor(int quality) {
        return getString(quality, COLOR_COMMON, COLOR_UNCOMMON, COLOR_RARE, COLOR_EPIC, COLOR_LEGENDARY, COLOR_SPECIAL);
    }

    private static String getString(int quality, String colorCommon, String colorUncommon, String colorRare, String colorEpic, String colorLegendary, String colorSpecial) {
        return switch (quality) {
            case 0 -> colorCommon;
            case 1 -> colorUncommon;
            case 2 -> colorRare;
            case 3 -> colorEpic;
            case 4 -> colorLegendary;
            default -> colorSpecial;
        };
    }

    public static String toNameKey(int quality) {
        return getString(quality, KEY_NAME_COMMON, KEY_NAME_UNCOMMON, KEY_NAME_RARE, KEY_NAME_EPIC, KEY_NAME_LEGENDARY, KEY_NAME_SPECIAL);
    }

    public static int levelToQuality(int level) {
        return Math.max(0, (level - 1) / 10);
    }

    public static String levelToColor(int level) {
        return toColor(levelToQuality(level));
    }
}
