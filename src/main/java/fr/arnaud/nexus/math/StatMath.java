package fr.arnaud.nexus.math;

public final class StatMath {

    private StatMath() {
    }

    public enum GrowthType {ADDITIVE, SCALAR}

    public static double additive(double base, double perLevel, int level) {
        return base + perLevel * (level - 1);
    }

    public static double scalar(double base, double perLevel, int level) {
        return base * (1.0 + perLevel * (level - 1));
    }

    public static double exponential(double base, double rate, int level) {
        return base * Math.pow(rate, level - 1);
    }

    public static double cappedValue(double raw, double cap) {
        return cap > 0 ? Math.min(raw, cap) : raw;
    }
}
