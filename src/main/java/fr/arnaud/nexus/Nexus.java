package fr.arnaud.nexus;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.AwakeningMarkerComponent;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.LucidityComponent;
import fr.arnaud.nexus.command.NexusTestCommand;
import fr.arnaud.nexus.component.WeaponSlotComponent;
import fr.arnaud.nexus.i18n.I18n;
import fr.arnaud.nexus.listener.PlayerSessionListener;
import fr.arnaud.nexus.system.DamageSystem;
import fr.arnaud.nexus.system.FlowSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

/**
 * Project Nexus: Gaia's Theft — Main plugin entrypoint.
 *
 * Lifecycle:
 *  setup()    → Register components, systems, global event listeners.
 *  start()    → Post-asset load init (stat indices resolved here via LoadedAssetsEvent).
 *  shutdown() → Cleanup.
 */
public final class Nexus extends JavaPlugin {

    private static Nexus instance;

    public Nexus(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static Nexus get() {
        return instance;
    }

    @Override
    protected void setup() {
        I18n.init(this);

        registerComponents();
        registerSystems();
        registerListeners();
        registerCommands();

        getLogger().at(Level.INFO).log(I18n.t("plugin.loaded"));
    }

    @Override
    protected void start() {
        // Custom stat indices (Flow generation rate, lucidity cap, etc.)
        // are resolved here via LoadedAssetsEvent — see FlowSystem.
        getLogger().at(Level.INFO).log(I18n.t("plugin.started"));
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log(I18n.t("plugin.stopped"));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void registerComponents() {
        // Pattern: registerComponent(Class, no-arg factory [, codec]).
        // The returned ComponentType is injected into the component class as a
        // static field so any code can call MyComponent.getComponentType()
        // without going through a plugin singleton.

        ComponentType<EntityStore, FlowComponent> flowType = getEntityStoreRegistry().registerComponent(
                FlowComponent.class, FlowComponent::new
        );
        FlowComponent.setComponentType(flowType);

        ComponentType<EntityStore, LucidityComponent> lucidityType = getEntityStoreRegistry().registerComponent(
                LucidityComponent.class, LucidityComponent::new
        );
        LucidityComponent.setComponentType(lucidityType);

        ComponentType<EntityStore, WeaponSlotComponent> weaponSlotType = getEntityStoreRegistry().registerComponent(
                WeaponSlotComponent.class, WeaponSlotComponent::new
        );
        WeaponSlotComponent.setComponentType(weaponSlotType);

        ComponentType<EntityStore, AwakeningMarkerComponent> awakeningType = getEntityStoreRegistry().registerComponent(
                AwakeningMarkerComponent.class, AwakeningMarkerComponent::new
        );
        AwakeningMarkerComponent.setComponentType(awakeningType);
    }

    private void registerSystems() {
        // FlowSystem — ticking lucidity drain for Unstable Sleep (co-op).
        // No constructor args; EntityTickingSystem filters via getQuery().
        getEntityStoreRegistry().registerSystem(new FlowSystem());

        // DamageSystem — ECS damage event handler (DamageEventSystem).
        // Handles: flow segment loss on hit received, flow gain on hit dealt.
        // Requires "Hytale:DamageModule" in manifest.json dependencies.
        getEntityStoreRegistry().registerSystem(new DamageSystem());
    }

    private void registerListeners() {
        // PlayerReadyEvent — bootstrap Nexus ECS components onto new players.
        getEventRegistry().registerGlobal(
                PlayerReadyEvent.class,
                PlayerSessionListener::onPlayerReady
        );

        // Damage events are handled by DamageSystem (DamageEventSystem),
        // registered above via registerSystem() — no global event needed here.
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new NexusTestCommand());
    }
}