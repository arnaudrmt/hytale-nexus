package fr.arnaud.nexus.ui.inventory;

/**
 * Maps a quality integer (or enchantment level used as quality) to a
 * hex colour string and a slot background image path.
 * <p>
 * Scale:
 * 0 → Common     → grey   #9d9d9d   Inventory/Slots/SlotCommon.png
 * 1 → Uncommon   → green  #1eff00   Inventory/Slots/SlotUncommon.png
 * 2 → Rare       → blue   #0070dd   Inventory/Slots/SlotRare.png
 * 3 → Epic       → purple #a335ee   Inventory/Slots/SlotEpic.png
 * 4 → Legendary  → gold   #ff8000   Inventory/Slots/SlotLegendary.png
 * 5+ → Developer → red    #ff0000   Inventory/Slots/SlotDeveloper.png
 */
public final class QualityMapper {

    // Hex colours
    public static final String COLOR_COMMON = "#9d9d9d";
    public static final String COLOR_UNCOMMON = "#1eff00";
    public static final String COLOR_RARE = "#0070dd";
    public static final String COLOR_EPIC = "#a335ee";
    public static final String COLOR_LEGENDARY = "#FFD700";
    public static final String COLOR_DEVELOPER = "#ff0000";

    // Slot image paths
    public static final String SLOT_COMMON = "Inventory/Slots/SlotCommon.png";
    public static final String SLOT_UNCOMMON = "Inventory/Slots/SlotUncommon.png";
    public static final String SLOT_RARE = "Inventory/Slots/SlotRare.png";
    public static final String SLOT_EPIC = "Inventory/Slots/SlotEpic.png";
    public static final String SLOT_LEGENDARY = "Inventory/Slots/SlotLegendary.png";
    public static final String SLOT_DEVELOPER = "Inventory/Slots/SlotDeveloper.png";

    // Quality names
    public static final String NAME_COMMON = "Common";
    public static final String NAME_UNCOMMON = "Uncommon";
    public static final String NAME_RARE = "Rare";
    public static final String NAME_EPIC = "Epic";
    public static final String NAME_LEGENDARY = "Legendary";
    public static final String NAME_DEVELOPER = "Developer";

    private QualityMapper() {
    }

    /**
     * Returns the hex colour string for the given quality value.
     */
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

    /**
     * Returns the slot background image path for the given quality value.
     */
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

    /**
     * Returns the name for the given quality value.
     */
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

    /**
     * Maps a weapon level to a quality tier for colour/slot purposes.
     * Levels 1-10 → quality 0 (Common), 11-20 → quality 1 (Uncommon), etc.
     */
    public static int levelToQuality(int level) {
        return Math.max(0, (level - 1) / 10);
    }

    /**
     * Convenience: colour for a weapon level.
     */
    public static String levelToColor(int level) {
        return toColor(levelToQuality(level));
    }

    /**
     * Convenience: slot image for a weapon level.
     */
    public static String levelToSlotImage(int level) {
        return toSlotImage(levelToQuality(level));
    }
}
