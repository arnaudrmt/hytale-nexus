package fr.arnaud.nexus.item.weapon.stats;

import java.util.HashMap;
import java.util.Map;

public final class WeaponStatRegistry {

    private static final WeaponStatRegistry INSTANCE = new WeaponStatRegistry();
    private final Map<Integer, WeaponStatCurves> curvesByQuality = new HashMap<>();

    private WeaponStatRegistry() {
    }

    public static WeaponStatRegistry get() {
        return INSTANCE;
    }

    public void registerCurves(int quality, WeaponStatCurves curves) {
        curvesByQuality.put(quality, curves);
    }

    public WeaponStatCurves getCurves(int quality) {
        return curvesByQuality.getOrDefault(quality, curvesByQuality.get(0));
    }

    public boolean isReady() {
        return !curvesByQuality.isEmpty();
    }
}
