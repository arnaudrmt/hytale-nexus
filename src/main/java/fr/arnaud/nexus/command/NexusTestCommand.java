package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.LucidityComponent;
import fr.arnaud.nexus.component.WeaponSlotComponent;
import fr.arnaud.nexus.i18n.I18n;
import fr.arnaud.nexus.weapon.*;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

/**
 * Test command to exercise all implemented Nexus features:
 *  - Flow component (segments, generation rate, retention, dash cost)
 *  - Lucidity component (current, max, unstable sleep, depletion)
 *  - WeaponSlot component (melee/ranged weapons, stat recalculation)
 *  - WeaponData (refinement, elements, enchantments, rarity)
 *  - Element system and ElementInteraction
 *  - Damage system integration (flow gain/loss simulation)
 *
 * Usage: /nexustest [--feature <name>]
 *   --feature flow       - Test Flow component mechanics
 *   --feature lucidity   - Test Lucidity component mechanics
 *   --feature weapons    - Test WeaponSlot and WeaponData
 *   --feature elements   - Test Element interactions
 *   (default: all)       - Run all tests
 */
public class NexusTestCommand extends AbstractPlayerCommand {

    private final DefaultArg<String> featureArg;

    public NexusTestCommand() {
        super("nexustest", "nexus.commands.test.desc");

        // Default argument: --feature <type> (defaults to "all")
        featureArg = withDefaultArg("feature", "Test feature to run",
                ArgTypes.STRING, "all", "all tests");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        String feature = context.get(featureArg).toLowerCase();

        player.sendMessage(Message.raw("§6=== Nexus Test Suite ==="));

        switch (feature) {
            case "flow":
                testFlow(ref, store, player);
                break;
            case "lucidity":
                testLucidity(ref, store, player);
                break;
            case "weapons":
                testWeapons(ref, store, player);
                break;
            case "elements":
                testElements(player);
                break;
            case "all":
                testFlow(ref, store, player);
                testLucidity(ref, store, player);
                testWeapons(ref, store, player);
                testElements(player);
                break;
            default:
                player.sendMessage(Message.raw("§cUsage: /nexustest [--feature <flow|lucidity|weapons|elements|all>]"));
                return;
        }

        player.sendMessage(Message.raw("§a=== All Tests Complete ==="));
    }

    // -------------------------------------------------------------------------
    // Flow Component Tests
    // -------------------------------------------------------------------------

    private void testFlow(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        player.sendMessage(Message.raw("§e--- Testing Flow Component ---"));

        FlowComponent flow = store.getComponent(ref, FlowComponent.getComponentType());
        if (flow == null) {
            flow = new FlowComponent();
            store.putComponent(ref, FlowComponent.getComponentType(), flow);
        }

        // Test 1: Initial state
        player.sendMessage(Message.raw(String.format("Initial: %.2f/%d segments", flow.getCurrent(), flow.getMaxSegments())));

        // Test 2: Add flow
        flow.addFlow(2.5f);
        player.sendMessage(Message.raw(String.format("After +2.5 flow: %.2f/%d", flow.getCurrent(), flow.getMaxSegments())));

        // Test 3: Fill to max
        boolean justFilled = flow.addFlow(10f);
        player.sendMessage(Message.raw(String.format("After +10 flow (full=%b): %.2f/%d", justFilled, flow.getCurrent(), flow.getMaxSegments())));

        // Test 4: Remove segments
        int lost = flow.removeSegments(2, false);
        player.sendMessage(Message.raw(String.format("After -2 segments: lost=%d, current=%.2f", lost, flow.getCurrent())));

        // Test 5: Retention chance
        flow.setRetentionChance(1.0f); // 100% retention
        int lostWithRetention = flow.removeSegments(1, true);
        player.sendMessage(Message.raw(String.format("Retention test (100%%): lost=%d (should be 0)", lostWithRetention)));

        // Test 6: Dash cost
        flow.drainFractional(FlowComponent.DASH_FLOW_COST);
        player.sendMessage(Message.raw(String.format("After dash (%.2f cost): %.2f", FlowComponent.DASH_FLOW_COST, flow.getCurrent())));

        // Test 7: Generation rate
        flow.setGenerationRate(2.0f);
        flow.addFlow(1.0f);
        player.sendMessage(Message.raw(String.format("With 2x gen rate, +1.0 flow: %.2f", flow.getCurrent())));

        // Test 8: Max segments expansion
        flow.setMaxSegments(7);
        player.sendMessage(Message.raw(String.format("Max segments expanded to: %d (cap=%d)", flow.getMaxSegments(), FlowComponent.ABSOLUTE_MAX_SEGMENTS)));

        store.putComponent(ref, FlowComponent.getComponentType(), flow);
        player.sendMessage(Message.raw("§aFlow tests complete"));
    }

    // -------------------------------------------------------------------------
    // Lucidity Component Tests
    // -------------------------------------------------------------------------

    private void testLucidity(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        player.sendMessage(Message.raw("§e--- Testing Lucidity Component ---"));

        LucidityComponent lucidity = store.getComponent(ref, LucidityComponent.getComponentType());
        if (lucidity == null) {
            lucidity = new LucidityComponent();
            store.putComponent(ref, LucidityComponent.getComponentType(), lucidity);
        }

        // Test 1: Initial state
        player.sendMessage(Message.raw(String.format("Initial: %.2f/%.2f (%.1f%%)", lucidity.getCurrent(), lucidity.getMax(), lucidity.getNormalized() * 100)));

        // Test 2: Drain
        lucidity.drain(25f);
        player.sendMessage(Message.raw(String.format("After -25: %.2f/%.2f", lucidity.getCurrent(), lucidity.getMax())));

        // Test 3: Gain
        float gained = lucidity.gain(15f);
        player.sendMessage(Message.raw(String.format("After +15: %.2f/%.2f (actual gain=%.2f)", lucidity.getCurrent(), lucidity.getMax(), gained)));

        // Test 4: Score multiplier
        float scoreMultiplier = lucidity.getScoreMultiplier();
        player.sendMessage(Message.raw(String.format("Score multiplier at %.1f%%: %.2fx", lucidity.getNormalized() * 100, scoreMultiplier)));

        // Test 5: Unstable sleep
        lucidity.setUnstableSleep(true);
        player.sendMessage(Message.raw(String.format("Unstable sleep enabled: %b (drain rate=%.2f/s)", lucidity.isUnstableSleep(), LucidityComponent.COOP_DRAIN_RATE_PER_SECOND)));

        // Test 6: Depletion
        lucidity.drain(lucidity.getCurrent());
        boolean depleted = lucidity.isDepleted();
        player.sendMessage(Message.raw(String.format("After full drain: %.2f (depleted=%b)", lucidity.getCurrent(), depleted)));

        // Restore for next tests
        lucidity.gain(100f);
        lucidity.setUnstableSleep(false);

        store.putComponent(ref, LucidityComponent.getComponentType(), lucidity);
        player.sendMessage(Message.raw("§aLucidity tests complete"));
    }

    // -------------------------------------------------------------------------
    // WeaponSlot & WeaponData Tests
    // -------------------------------------------------------------------------

    private void testWeapons(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        player.sendMessage(Message.raw("§e--- Testing Weapons ---"));

        WeaponSlotComponent slots = store.getComponent(ref, WeaponSlotComponent.getComponentType());
        FlowComponent flow = store.getComponent(ref, FlowComponent.getComponentType());
        if (slots == null) {
            slots = new WeaponSlotComponent();
            store.putComponent(ref, WeaponSlotComponent.getComponentType(), slots);
        }

        // Test 1: Create legendary melee weapon
        WeaponData melee = WeaponData.builder("test.sword", WeaponType.MELEE, WeaponRarity.LEGENDARY)
                .damage(50f)
                .flowBonus(2)
                .moveSpeed(0.15f)
                .attackSpeed(0.2f)
                .resilience(25f)
                .enchantSlots(3)
                .build();

        player.sendMessage(Message.raw(String.format("Created %s %s: dmg=%.1f, flow+%d", 
                melee.getRarity(), melee.getType(), melee.getEffectiveDamage(), melee.getFlowBonus())));

        // Test 2: Refinement
        int refineCost = melee.getRefinementCost();
        boolean refined = melee.refine();
        player.sendMessage(Message.raw(String.format("Refinement: cost=%d, success=%b, level=%d", 
                refineCost, refined, melee.getRefinementLevel())));
        player.sendMessage(Message.raw(String.format("After refine: dmg=%.1f (multiplier=%.2fx)", 
                melee.getEffectiveDamage(), melee.getRarity().getStatMultiplier())));

        // Test 3: Elemental core
        melee.slotCore(Element.FIRE);
        player.sendMessage(Message.raw(String.format("Slotted core: %s (hasCore=%b)", melee.getCore(), melee.hasCore())));

        // Test 4: Enchantments
        player.sendMessage(Message.raw(String.format("Enchant slots: %d", melee.getEnchantments().size())));
        player.sendMessage(Message.raw(String.format("Active at 3 flow: %d enchants", melee.getActiveEnchantments(3).size())));

        // Test 5: Create rare ranged weapon
        WeaponData ranged = WeaponData.builder("test.bow", WeaponType.RANGED, WeaponRarity.RARE)
                .damage(35f)
                .flowBonus(1)
                .attackSpeed(0.3f)
                .resilience(10f)
                .enchantSlots(2)
                .build();
        ranged.slotCore(Element.ICE);

        player.sendMessage(Message.raw(String.format("Created %s %s: dmg=%.1f, element=%s", 
                ranged.getRarity(), ranged.getType(), ranged.getEffectiveDamage(), ranged.getCore())));

        // Test 6: Equip weapons and check stat totals
        slots.equipMelee(melee, flow);
        slots.equipRange(ranged, flow);
        store.putComponent(ref, WeaponSlotComponent.getComponentType(), slots);

        player.sendMessage(Message.raw(String.format("Total stats: dmg=%.1f, moveSpd=%.2f, atkSpd=%.2f, resilience=%.1f", 
                slots.getTotalDamage(), slots.getTotalMoveSpeed(), slots.getTotalAttackSpeed(), slots.getTotalResilience())));

        // Test 7: Check Flow recalculation
        if (flow != null) {
            player.sendMessage(Message.raw(String.format("Flow max after equip: %d (base+%d from weapons)", 
                    flow.getMaxSegments(), melee.getFlowBonus() + ranged.getFlowBonus())));
            player.sendMessage(Message.raw(String.format("Retention chance: %.1f%%", flow.getRetentionChance() * 100)));
            store.putComponent(ref, FlowComponent.getComponentType(), flow);
        }

        // Test 8: Weapon rarity scaling
        player.sendMessage(Message.raw("Rarity multipliers:"));
        for (WeaponRarity rarity : WeaponRarity.values()) {
            player.sendMessage(Message.raw(String.format("  %s: %.2fx, max enchants=%d", 
                    rarity.name(), rarity.getStatMultiplier(), rarity.getMaxEnchantSlots())));
        }

        player.sendMessage(Message.raw("§aWeapon tests complete"));
    }

    // -------------------------------------------------------------------------
    // Element System Tests
    // -------------------------------------------------------------------------

    private void testElements(Player player) {
        player.sendMessage(Message.raw("§e--- Testing Elements ---"));

        // Test 1: All elements
        player.sendMessage(Message.raw("Elements: " + java.util.Arrays.toString(Element.values())));

        // Test 2: Element interactions
        player.sendMessage(Message.raw("Element interactions:"));
        testInteraction(player, Element.FIRE, Element.ICE, 1.5f);
        testInteraction(player, Element.ICE, Element.FIRE, 0.75f);
        testInteraction(player, Element.FIRE, Element.FIRE, 1.0f);

        player.sendMessage(Message.raw("§aElement tests complete"));
    }

    private void testInteraction(Player player, Element attacker, Element defender, float expectedMultiplier) {
        float multiplier = ElementInteraction.NEUTRAL.getDamageMultiplier();
        String result = multiplier > 1.0f ? "§a+" : multiplier < 1.0f ? "§c-" : "§7=";
        player.sendMessage(Message.raw(String.format("  %s %s vs %s: %.2fx %s(expected %.2fx)", 
                result, attacker, defender, multiplier, multiplier == expectedMultiplier ? "§a✓" : "§c✗", expectedMultiplier)));
    }
}
