package fr.arnaud.nexus;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import fr.arnaud.nexus.breach.*;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraSystem;
import fr.arnaud.nexus.command.NexusTestCommand;
import fr.arnaud.nexus.component.CursorTargetComponent;
import fr.arnaud.nexus.component.DashComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.handler.DreamDustHandler;
import fr.arnaud.nexus.handler.FlowHandler;
import fr.arnaud.nexus.i18n.I18n;
import fr.arnaud.nexus.level.LevelManager;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.level.LevelWorldLoadSystem;
import fr.arnaud.nexus.listener.InputListener;
import fr.arnaud.nexus.listener.PlayerSessionListener;
import fr.arnaud.nexus.spawning.MobSpawnManager;
import fr.arnaud.nexus.spawning.SpawnerMobDeathSystem;
import fr.arnaud.nexus.spawning.SpawnerProximitySystem;
import fr.arnaud.nexus.spawning.SpawnerTagComponent;
import fr.arnaud.nexus.switchstrike.*;
import fr.arnaud.nexus.system.DashSystem;
import fr.arnaud.nexus.system.PlayerMovementSystem;
import fr.arnaud.nexus.system.SlotLockSystem;
import fr.arnaud.nexus.ui.hud.FlowDebugHudSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public final class Nexus extends JavaPlugin {

    private static Nexus instance;

    private final FlowHandler flowHandler = new FlowHandler();
    private final DreamDustHandler dreamDustHandler = new DreamDustHandler();
    private final DashSystem dashSystem = new DashSystem(flowHandler);
    private final InputListener inputListener = new InputListener(dashSystem);
    private final SwitchStrikeTriggerSystem switchStrikeTriggerSystem = new SwitchStrikeTriggerSystem();
    private final SwitchStrikeExecutionSystem switchStrikeExecutionSystem = new SwitchStrikeExecutionSystem(flowHandler);
    private final FlowDebugHudSystem flowDebugHudSystem = new FlowDebugHudSystem();
    private final LevelWorldLoadSystem levelWorldLoadSystem = new LevelWorldLoadSystem();
    private final LevelManager levelManager = new LevelManager();
    private final MobSpawnManager mobSpawnManager = new MobSpawnManager();


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
        SwitchStrikeComponent.setComponentType(
            registry.registerComponent(SwitchStrikeComponent.class, SwitchStrikeComponent::new)
        );
        FrozenComponent.setComponentType(
            registry.registerComponent(FrozenComponent.class, FrozenComponent::new)
        );
        BreachSequenceComponent.setComponentType(
            registry.registerComponent(BreachSequenceComponent.class, BreachSequenceComponent::new)
        );
        RunSessionComponent.setComponentType(
            registry.registerComponent(RunSessionComponent.class, "Nexus_RunSession", RunSessionComponent.CODEC)
        );
        SpawnerTagComponent.setComponentType(
            registry.registerComponent(SpawnerTagComponent.class, "Nexus_SpawnerTag", SpawnerTagComponent.CODEC)
        );
        LevelProgressComponent.setComponentType(
            registry.registerComponent(LevelProgressComponent.class, "Nexus_LevelProgress", LevelProgressComponent.CODEC)
        );
    }

    private void registerSystems() {
        var entityRegistry = getEntityStoreRegistry();
        entityRegistry.registerSystem(new CameraSystem());
        entityRegistry.registerSystem(new PlayerMovementSystem());
        entityRegistry.registerSystem(switchStrikeTriggerSystem);
        entityRegistry.registerSystem(switchStrikeExecutionSystem);
        entityRegistry.registerSystem(flowDebugHudSystem);
        entityRegistry.registerSystem(new SwitchStrikeBossHitSystem());
        entityRegistry.registerSystem(new BreachSequenceSystem(flowHandler));
        entityRegistry.registerSystem(new BreachDamageInterceptor());
        entityRegistry.registerSystem(new BreachFreezeSystem());
        entityRegistry.registerSystem(new BreachFreezeAttackInterceptor());
        entityRegistry.registerSystem(new SpawnerProximitySystem());
        entityRegistry.registerSystem(new SpawnerMobDeathSystem());
        new SlotLockSystem();
        new SwitchStrikePacketInterceptor();
    }

    private void registerListeners() {
        var events = getEventRegistry();
        events.register(LoadedAssetsEvent.class, EntityStatType.class, flowHandler::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, dreamDustHandler::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, switchStrikeTriggerSystem::onAssetsLoaded);
        events.registerGlobal(StartWorldEvent.class, levelWorldLoadSystem::onWorldStart);
        events.registerGlobal(PlayerReadyEvent.class, PlayerSessionListener::onPlayerReady);
        events.registerGlobal(PlayerMouseButtonEvent.class, inputListener::onMouseButton);
        events.registerGlobal(PlayerReadyEvent.class, FlowDebugHudSystem::onPlayerReady);
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new NexusTestCommand());
    }

    public LevelWorldLoadSystem getLevelWorldLoadSystem() {
        return levelWorldLoadSystem;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public MobSpawnManager getMobSpawnManager() {
        return mobSpawnManager;
    }

}
