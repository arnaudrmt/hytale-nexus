package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.server.core.Message;

public final class QualityMapper {

    public static final String COLOR_COMMON = "#9d9d9d";
    public static final String COLOR_UNCOMMON = "#1eff00";
    public static final String COLOR_RARE = "#0070dd";
    public static final String COLOR_EPIC = "#a335ee";
    public static final String COLOR_LEGENDARY = "#FFD700";
    public static final String COLOR_DEVELOPER = "#ff0000";

    public static final String NAME_COMMON = Message.translation("nexus.quality.common").getRawText();
    public static final String NAME_UNCOMMON = Message.translation("nexus.quality.uncommon").getRawText();
    public static final String NAME_RARE = Message.translation("nexu.quality.rare").getRawText();
    public static final String NAME_EPIC = Message.translation("nexu.quality.epic").getRawText();
    public static final String NAME_LEGENDARY = Message.translation("nexu.quality.legendary").getRawText();
    public static final String NAME_DEVELOPER = Message.translation("nexu.quality.developer").getRawText();

    private QualityMapper() {
    }

    public static String toColor(int quality) {
        return getString(quality, COLOR_COMMON, COLOR_UNCOMMON, COLOR_RARE, COLOR_EPIC, COLOR_LEGENDARY, COLOR_DEVELOPER);
    }

    public static String toName(int quality) {
        return getString(quality, NAME_COMMON, NAME_UNCOMMON, NAME_RARE, NAME_EPIC, NAME_LEGENDARY, NAME_DEVELOPER);
    }

    private static String getString(int quality, String nameCommon, String nameUncommon, String nameRare, String nameEpic, String nameLegendary, String nameDeveloper) {
        return switch (quality) {
            case 0 -> nameCommon;
            case 1 -> nameUncommon;
            case 2 -> nameRare;
            case 3 -> nameEpic;
            case 4 -> nameLegendary;
            default -> nameDeveloper;
        };
    }

    public static int levelToQuality(int level) {
        return Math.max(0, (level - 1) / 10);
    }

    public static String levelToColor(int level) {
        return toColor(levelToQuality(level));
    }
}
