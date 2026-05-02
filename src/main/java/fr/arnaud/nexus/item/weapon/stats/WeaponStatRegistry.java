package fr.arnaud.nexus.item.weapon.stats;

import java.util.HashMap;
import java.util.Map;

public final class WeaponStatRegistry {

    private static final WeaponStatRegistry INSTANCE = new WeaponStatRegistry();
    private final Map<Integer, WeaponStatCurves> curvesByQualityValue = new HashMap<>();

    private WeaponStatRegistry() {
    }

    public static WeaponStatRegistry getInstance() {
        return INSTANCE;
    }

    public void registerCurves(int quality, WeaponStatCurves curves) {
        curvesByQualityValue.put(quality, curves);
    }

    public WeaponStatCurves getCurves(int quality) {
        return curvesByQualityValue.getOrDefault(quality, curvesByQualityValue.get(0));
    }

    public boolean isReady() {
        return !curvesByQualityValue.isEmpty();
    }
}
