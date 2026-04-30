package fr.arnaud.nexus.item.weapon.generator;

import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;

import java.util.List;
import java.util.stream.Collectors;

public final class EnchantmentPoolService {

    public List<EnchantmentDefinition> getPoolForTag(WeaponTag tag) {
        return EnchantmentRegistry.get()
                                  .getAllDefinitions()
                                  .stream()
                                  .filter(def -> def.getCompatibleTag().isCompatibleWith(tag))
                                  .collect(Collectors.toList());
    }
}
