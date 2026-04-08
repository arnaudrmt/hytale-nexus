package fr.arnaud.nexus.weapon.enchantment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EnchantmentRegistry {

    private static final EnchantmentRegistry INSTANCE = new EnchantmentRegistry();

    private final Map<String, EnchantmentDefinition> definitionsById = new HashMap<>();
    private final Map<String, EnchantmentHandler> handlersById = new HashMap<>();

    private EnchantmentRegistry() {
    }

    public static EnchantmentRegistry get() {
        return INSTANCE;
    }

    public void registerDefinition(EnchantmentDefinition definition) {
        definitionsById.put(definition.getEnchantmentId(), definition);
    }

    public void registerHandler(String behaviorId, EnchantmentHandler handler) {
        handlersById.put(behaviorId, handler);
    }

    public EnchantmentDefinition getDefinition(String enchantmentId) {
        return definitionsById.get(enchantmentId);
    }

    public Collection<EnchantmentDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitionsById.values());
    }

    public EnchantmentHandler getHandler(String enchantmentId) {
        EnchantmentDefinition def = definitionsById.get(enchantmentId);
        if (def == null) return null;
        if (def.isBehavioral()) return handlersById.get(def.getBehaviorId());
        return handlersById.get(enchantmentId);
    }

    public boolean hasDefinition(String enchantmentId) {
        return definitionsById.containsKey(enchantmentId);
    }
}
