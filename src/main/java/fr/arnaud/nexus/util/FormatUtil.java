package fr.arnaud.nexus.util;

import java.util.Locale;

public class FormatUtil {

    // Always rounds to the nearest whole number. 1.8 -> "2"
    public static String formatAsInteger(float value) {
        return String.valueOf(Math.round(value));
    }

    // Returns an integer string if whole, otherwise formats to 1 decimal place. 5.0 -> "5", 5.55 -> "5.6"
    public static String formatSmartDecimal(float value) {
        if (value == Math.floor(value) && !Float.isInfinite(value))
            return String.valueOf((int) value);
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // Converts a multiplier into a signed percentage relative to 1.0. 1.2 -> "+20%", 0.8 -> "-20%"
    public static String formatPercentModifier(double multiplier) {
        long pct = Math.round((multiplier - 1.0) * 100.0);
        if (pct == 0) return "0%";
        return String.format(Locale.ROOT, "%+d%%", pct);
    }

    // Returns a signed (+/-) string. Hides decimal if whole, otherwise 1 decimal. 5.0 -> "+5", 5.5 -> "+5.5"
    public static String formatSignedSmart(double value) {
        if (value == 0) return "0";
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.format(Locale.ROOT, "%+d", (int) value);
        }
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    // Formats a delta as either a signed percentage (1 decimal) or a signed number. 0.15, true -> "+15.0%"
    public static String formatChange(double delta, boolean asPercentage) {
        if (asPercentage) {
            return String.format(Locale.ROOT, "%+.1f", delta * 100.0);
        }
        return formatSignedSmart(delta);
    }

    // Rounds to a whole number and adds spaces as thousands separators. 1250 -> "1 250"
    public static String formatGroupedInteger(float value) {
        int rounded = Math.round(value);
        if (rounded < 1000 && rounded > -1000) {
            return String.valueOf(rounded);
        }
        return String.format(Locale.ROOT, "%,d", rounded).replace(',', ' ');
    }
}
