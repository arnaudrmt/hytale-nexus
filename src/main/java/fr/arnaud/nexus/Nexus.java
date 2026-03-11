package fr.arnaud.nexus;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraSystem;
import fr.arnaud.nexus.command.NexusTestCommand;
import fr.arnaud.nexus.component.*;
import fr.arnaud.nexus.event.SwitchStrikeActivatedEvent;
import fr.arnaud.nexus.handler.DreamDustHandler;
import fr.arnaud.nexus.handler.FlowHandler;
import fr.arnaud.nexus.handler.LucidityHandler;
import fr.arnaud.nexus.handler.SwitchStrikeHandler;
import fr.arnaud.nexus.i18n.I18n;
import fr.arnaud.nexus.listener.InputListener;
import fr.arnaud.nexus.listener.PlayerSessionListener;
import fr.arnaud.nexus.system.DashSystem;
import fr.arnaud.nexus.system.PlayerMovementSystem;
import fr.arnaud.nexus.system.SwitchStrikeTriggerSystem;
import fr.arnaud.nexus.system.WeaponSwapSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class Nexus extends JavaPlugin {

    private static Nexus instance;

    private final FlowHandler flowHandler = new FlowHandler();
    private final LucidityHandler lucidityHandler = new LucidityHandler();
    private final DreamDustHandler dreamDustHandler = new DreamDustHandler();
    private final SwitchStrikeHandler switchStrikeHandler = new SwitchStrikeHandler();
    private final WeaponSwapSystem weaponSwapSystem = new WeaponSwapSystem();
    private final DashSystem dashSystem = new DashSystem(flowHandler);
    private final InputListener inputListener = new InputListener(dashSystem, weaponSwapSystem);

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
        var registry = getEntityStoreRegistry();

        CameraComponent.setComponentType(registry.registerComponent(CameraComponent.class, CameraComponent::new));
        PlayerBodyComponent.setComponentType(registry.registerComponent(PlayerBodyComponent.class, PlayerBodyComponent::new));
        DashComponent.setComponentType(registry.registerComponent(DashComponent.class, DashComponent::new));
        CursorTargetComponent.setComponentType(registry.registerComponent(CursorTargetComponent.class, CursorTargetComponent::new));
        SwitchStrikeComponent.setComponentType(registry.registerComponent(SwitchStrikeComponent.class, SwitchStrikeComponent::new));

        RunSessionComponent.setComponentType(
            registry.registerComponent(RunSessionComponent.class, "Nexus_RunSession", RunSessionComponent.CODEC)
        );
        EquippedWeaponsComponent.setComponentType(
            registry.registerComponent(EquippedWeaponsComponent.class, "Nexus_EquippedWeapons", EquippedWeaponsComponent.CODEC)
        );
    }

    private void registerSystems() {
        var entityRegistry = getEntityStoreRegistry();
        entityRegistry.registerSystem(new CameraSystem());
        entityRegistry.registerSystem(new PlayerMovementSystem());

        // LoadedAssetsEvent fires on every asset reload pass. The AtomicBoolean ensures
        // SwitchStrikeTriggerSystem is only registered once for the lifetime of this plugin.
        AtomicBoolean triggerSystemRegistered = new AtomicBoolean(false);
        getEventRegistry().register(
            LoadedAssetsEvent.class,
            EntityStatType.class,
            (LoadedAssetsEvent<String, EntityStatType, IndexedLookupTableAssetMap<String, EntityStatType>> event) -> {
                if (!triggerSystemRegistered.compareAndSet(false, true)) return;
                int triggerIndex = event.getAssetMap().getIndex("SwitchStrike_Trigger");
                entityRegistry.registerSystem(new SwitchStrikeTriggerSystem(triggerIndex));
            }
        );
    }

    private void registerListeners() {
        var events = getEventRegistry();
        events.register(LoadedAssetsEvent.class, EntityStatType.class, flowHandler::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, lucidityHandler::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, dreamDustHandler::onAssetsLoaded);
        events.registerGlobal(PlayerReadyEvent.class, PlayerSessionListener::onPlayerReady);
        events.registerGlobal(PlayerMouseButtonEvent.class, inputListener::onMouseButton);
        events.registerGlobal(SwitchStrikeActivatedEvent.class, switchStrikeHandler);
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new NexusTestCommand());
    }
}
