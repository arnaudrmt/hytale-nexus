package fr.arnaud.nexus.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FormatUtil {

    public static String formatDuration(long ms) {
        long h = TimeUnit.MILLISECONDS.toHours(ms);
        long m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        if (h > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    public static String formatDecimal(float value) {
        if (value == Math.floor(value) && !Float.isInfinite(value))
            return String.valueOf((int) value);
        return String.format("%.1f", value);
    }

    public static String formatDamage(double multiplier) {
        double pct = (multiplier - 1.0) * 100.0;
        if (pct == 0) return "0%";
        return (pct > 0 ? "+" : "") + Math.round(pct) + "%";
    }

    public static String formatFlat(double value) {
        if (value == 0) return "0";
        if (value == Math.floor(value))
            return (value > 0 ? "+" : "") + (int) value;
        return String.format("%+.1f", value);
    }

    public static String formatDelta(double delta, boolean isMultiplier) {
        if (isMultiplier) {
            return String.format("%+.1f%%", delta * 100.0);
        }
        if (delta == Math.floor(delta))
            return String.format("%+d", (int) delta);
        return String.format("%+.1f", delta);
    }

    public static String formatCost(float cost) {
        int c = Math.round(cost);
        if (c < 1000) return String.valueOf(c);
        return (c / 1000) + " " + String.format("%03d", c % 1000);
    }
}
