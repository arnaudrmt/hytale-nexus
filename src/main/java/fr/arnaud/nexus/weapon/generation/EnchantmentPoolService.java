package fr.arnaud.nexus.weapon.generation;

import fr.arnaud.nexus.weapon.data.WeaponTag;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentRegistry;

import java.util.List;
import java.util.stream.Collectors;

public final class EnchantmentPoolService {

    public List<EnchantmentDefinition> getPoolForTag(WeaponTag tag, int level) {
        return EnchantmentRegistry.get()
                                  .getAllDefinitions()
                                  .stream()
                                  .filter(def -> def.getLevel() == level)
                                  .filter(def -> def.getCompatibleTag().isCompatibleWith(tag))
                                  .collect(Collectors.toList());
    }
}
