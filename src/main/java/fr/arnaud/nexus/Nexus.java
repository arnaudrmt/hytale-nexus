package fr.arnaud.nexus;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraSystem;
import fr.arnaud.nexus.command.NexusTestCommand;
import fr.arnaud.nexus.component.*;
import fr.arnaud.nexus.i18n.I18n;
import fr.arnaud.nexus.listener.DashInputListener;
import fr.arnaud.nexus.listener.PlayerSessionListener;
import fr.arnaud.nexus.system.FlowSystem;
import fr.arnaud.nexus.system.PlayerMovementSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

/**
 * Main entry point for Project Nexus.
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
        getLogger().at(Level.INFO).log(I18n.t("plugin.started"));
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log(I18n.t("plugin.stopped"));
    }

    private void registerComponents() {
        register(FlowComponent.class, FlowComponent::new, FlowComponent::setComponentType);
        register(LucidityComponent.class, LucidityComponent::new, LucidityComponent::setComponentType);
        register(RunSessionComponent.class, RunSessionComponent::new, RunSessionComponent::setComponentType);
        register(DreamDustComponent.class, DreamDustComponent::new, DreamDustComponent::setComponentType);
        register(CameraComponent.class, CameraComponent::new, CameraComponent::setComponentType);
        register(PlayerBodyComponent.class, PlayerBodyComponent::new, PlayerBodyComponent::setComponentType);
    }

    private void registerSystems() {
        var registry = getEntityStoreRegistry();
        registry.registerSystem(new FlowSystem());
        registry.registerSystem(new CameraSystem());
        registry.registerSystem(new PlayerMovementSystem());
    }

    private void registerListeners() {
        var events = getEventRegistry();
        events.registerGlobal(PlayerReadyEvent.class, PlayerSessionListener::onPlayerReady);
        events.registerGlobal(PlayerMouseButtonEvent.class, DashInputListener::onMouseButton);
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new NexusTestCommand());
    }

    /**
     * Helper to reduce boilerplate during component registration.
     */
    private <T extends com.hypixel.hytale.component.Component<EntityStore>> void register(
        Class<T> clazz, java.util.function.Supplier<T> supplier,
        java.util.function.Consumer<ComponentType<EntityStore, T>> setter) {

        var type = getEntityStoreRegistry().registerComponent(clazz, supplier);
        setter.accept(type);
    }
}
