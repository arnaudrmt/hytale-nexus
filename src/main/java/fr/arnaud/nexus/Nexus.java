package fr.arnaud.nexus;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraSystem;
import fr.arnaud.nexus.command.NexusTestCommand;
import fr.arnaud.nexus.command.StartRunCommand;
import fr.arnaud.nexus.component.CursorTargetComponent;
import fr.arnaud.nexus.component.DashComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.core.DreamDustHandler;
import fr.arnaud.nexus.core.FlowHandler;
import fr.arnaud.nexus.core.LucidityHandler;
import fr.arnaud.nexus.i18n.I18n;
import fr.arnaud.nexus.listener.InputListener;
import fr.arnaud.nexus.listener.PlayerSessionListener;
import fr.arnaud.nexus.system.DashSystem;
import fr.arnaud.nexus.system.PlayerMovementSystem;
import fr.arnaud.nexus.ui.hud.NexusHudSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public final class Nexus extends JavaPlugin {

    private static Nexus instance;

    private final FlowHandler flowHandler = new FlowHandler();
    private final LucidityHandler lucidityHandler = new LucidityHandler();
    private final DreamDustHandler dreamDustHandler = new DreamDustHandler();
    private final DashSystem dashSystem = new DashSystem(flowHandler);
    private final InputListener inputListener = new InputListener(dashSystem);

    public Nexus(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static Nexus get() {
        return instance;
    }

    public FlowHandler getFlowHandler() {
        return flowHandler;
    }

    public LucidityHandler getLucidityHandler() {
        return lucidityHandler;
    }

    public DreamDustHandler getDreamDustHandler() {
        return dreamDustHandler;
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
        register(CameraComponent.class, CameraComponent::new, CameraComponent::setComponentType);
        register(PlayerBodyComponent.class, PlayerBodyComponent::new, PlayerBodyComponent::setComponentType);
        register(DashComponent.class, DashComponent::new, DashComponent::setComponentType);
        register(RunSessionComponent.class, RunSessionComponent::new, RunSessionComponent::setComponentType);
        register(CursorTargetComponent.class, CursorTargetComponent::new, CursorTargetComponent::setComponentType);
    }

    private void registerSystems() {
        var registry = getEntityStoreRegistry();
        registry.registerSystem(new CameraSystem());
        registry.registerSystem(new PlayerMovementSystem());
    }

    private void registerListeners() {
        var events = getEventRegistry();
        events.register(LoadedAssetsEvent.class, EntityStatType.class, flowHandler::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, lucidityHandler::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, dreamDustHandler::onAssetsLoaded);
        events.registerGlobal(PlayerReadyEvent.class, PlayerSessionListener::onPlayerReady);
        events.registerGlobal(PlayerReadyEvent.class, NexusHudSystem::onPlayerReady);
        events.registerGlobal(PlayerMouseButtonEvent.class, inputListener::onMouseButton);
    }

    private void registerCommands() {
        var reg = getCommandRegistry();
        reg.registerCommand(new NexusTestCommand());
        reg.registerCommand(new StartRunCommand());
    }

    private <T extends com.hypixel.hytale.component.Component<EntityStore>> void register(
        Class<T> clazz, java.util.function.Supplier<T> supplier,
        java.util.function.Consumer<ComponentType<EntityStore, T>> setter) {
        setter.accept(getEntityStoreRegistry().registerComponent(clazz, supplier));
    }
}
