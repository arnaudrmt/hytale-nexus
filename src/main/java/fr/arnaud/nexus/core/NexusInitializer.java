package fr.arnaud.nexus.core;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import fr.arnaud.nexus.camera.CameraOcclusionSystem;
import fr.arnaud.nexus.camera.PlayerCameraComponent;
import fr.arnaud.nexus.camera.PlayerCameraSystem;
import fr.arnaud.nexus.camera.PlayerOcclusionComponent;
import fr.arnaud.nexus.command.AdminStatsCommand;
import fr.arnaud.nexus.command.AdminWeaponCommand;
import fr.arnaud.nexus.command.OpenInventoryCommand;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.feature.breach.*;
import fr.arnaud.nexus.feature.combat.HeadLockComponent;
import fr.arnaud.nexus.feature.combat.HeadTrackingSystem;
import fr.arnaud.nexus.feature.combat.PlayerBodyStateComponent;
import fr.arnaud.nexus.feature.combat.PlayerLocomotionSystem;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeBossHitSystem;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeComponent;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikePacketInterceptor;
import fr.arnaud.nexus.feature.movement.PlayerDashComponent;
import fr.arnaud.nexus.input.PlayerCursorTargetComponent;
import fr.arnaud.nexus.input.hover.PlayerHoverStateComponent;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDamageInterceptor;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistrar;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatConfigLoader;
import fr.arnaud.nexus.item.weapon.system.PlayerWeaponInitSystem;
import fr.arnaud.nexus.item.weapon.system.WeaponSwapSystem;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.spawner.SpawnerMobDeathSystem;
import fr.arnaud.nexus.spawner.SpawnerProximitySystem;
import fr.arnaud.nexus.spawner.SpawnerTagComponent;
import fr.arnaud.nexus.system.NexusStoragePickupGuard;

public final class NexusInitializer {

    private final Nexus plugin;

    public NexusInitializer(Nexus plugin) {
        this.plugin = plugin;
    }

    public void init() {
        initializeLoaders();
        registerComponents();
        registerSystems();
        registerListeners();
        registerCommands();
    }

    private void initializeLoaders() {
        WeaponStatConfigLoader.load();

        EnchantmentRegistry.get().loadAll();
        EnchantmentRegistrar.registerAll();
    }

    private void registerComponents() {
        var registry = plugin.getEntityStoreRegistry();

        HeadLockComponent.setComponentType(registry.registerComponent(HeadLockComponent.class, HeadLockComponent::new));

        RunSessionComponent.setComponentType(registry.registerComponent(RunSessionComponent.class, "Nexus_RunSession", RunSessionComponent.CODEC));

        PlayerCameraComponent.setComponentType(registry.registerComponent(PlayerCameraComponent.class, PlayerCameraComponent::new));
        PlayerOcclusionComponent.setComponentType(registry.registerComponent(PlayerOcclusionComponent.class, PlayerOcclusionComponent::new));

        PlayerBodyStateComponent.setComponentType(registry.registerComponent(PlayerBodyStateComponent.class, PlayerBodyStateComponent::new));
        PlayerDashComponent.setComponentType(registry.registerComponent(PlayerDashComponent.class, PlayerDashComponent::new));

        PlayerCursorTargetComponent.setComponentType(registry.registerComponent(PlayerCursorTargetComponent.class, PlayerCursorTargetComponent::new));
        PlayerHoverStateComponent.setComponentType(registry.registerComponent(PlayerHoverStateComponent.class, PlayerHoverStateComponent::new));

        SwitchStrikeComponent.setComponentType(registry.registerComponent(SwitchStrikeComponent.class, SwitchStrikeComponent::new));
        FrozenTargetComponent.setComponentType(registry.registerComponent(FrozenTargetComponent.class, FrozenTargetComponent::new));
        BreachSequenceComponent.setComponentType(registry.registerComponent(BreachSequenceComponent.class, BreachSequenceComponent::new));

        LevelProgressComponent.setComponentType(registry.registerComponent(LevelProgressComponent.class, "Nexus_LevelProgress", LevelProgressComponent.CODEC));
        SpawnerTagComponent.setComponentType(registry.registerComponent(SpawnerTagComponent.class, "Nexus_SpawnerTag", SpawnerTagComponent.CODEC));

        WeaponInstanceComponent.setComponentType(registry.registerComponent(WeaponInstanceComponent.class, WeaponInstanceComponent::new));
        PlayerWeaponStateComponent.setComponentType(
            registry.registerComponent(
                PlayerWeaponStateComponent.class,
                "Nexus_PlayerWeaponState",
                PlayerWeaponStateComponent.CODEC
            )
        );

        registry.registerComponent(InventoryComponent.Storage.class, InventoryComponent.Storage::new);
    }

    private void registerSystems() {
        var registry = plugin.getEntityStoreRegistry();

        registry.registerSystem(new HeadTrackingSystem());

        registry.registerSystem(new PlayerCameraSystem());
        registry.registerSystem(new CameraOcclusionSystem());

        registry.registerSystem(new PlayerLocomotionSystem());

        registry.registerSystem(new SwitchStrikeBossHitSystem());
        new SwitchStrikePacketInterceptor();
        registry.registerSystem(plugin.getSwitchStrikeTriggerSystem());
        registry.registerSystem(plugin.getSwitchStrikeExecutionSystem());

        // Three enchant damage systems replacing the old single interceptor
        registry.registerSystem(new EnchantmentDamageInterceptor.OnHitSystem());
        registry.registerSystem(new EnchantmentDamageInterceptor.OnReceiveHitSystem());
        registry.registerSystem(new EnchantmentDamageInterceptor.OnKillSystem());

        registry.registerSystem(new BreachSequenceSystem());
        registry.registerSystem(new BreachDamageInterceptor());
        registry.registerSystem(new BreachFreezeSystem());
        registry.registerSystem(new BreachFreezeAttackInterceptor());

        registry.registerSystem(new SpawnerProximitySystem());
        registry.registerSystem(new SpawnerMobDeathSystem());

        registry.registerSystem(new PlayerWeaponInitSystem(plugin.getWeaponEquipSystem()));

        registry.registerSystem(new NexusStoragePickupGuard(
            EntityModule.get().getPlayerSpatialResourceType()
        ));

        new WeaponSwapSystem(plugin.getWeaponEquipSystem());
    }

    private void registerListeners() {
        var events = plugin.getEventRegistry();

        events.registerGlobal(StartWorldEvent.class, plugin.getNexusWorldLoadSystem()::onWorldStart);
        events.registerGlobal(PlayerReadyEvent.class, PlayerSessionListener::onPlayerReady);

        events.register(LoadedAssetsEvent.class, EntityStatType.class, plugin.getPlayerStatsManager()::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, plugin.getSwitchStrikeTriggerSystem()::onAssetsLoaded);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, plugin.getStatIndexResolver()::onAssetsLoaded);

        events.registerGlobal(PlayerMouseButtonEvent.class, plugin.getPlayerInputListener()::onMouseButton);
        events.registerGlobal(PlayerMouseMotionEvent.class, plugin.getPlayerMouseMotionListener()::onMouseMotion);

        plugin.getInventoryPacketInterceptor().register();
    }

    private void registerCommands() {
        var registry = plugin.getCommandRegistry();

        registry.registerCommand(new AdminStatsCommand());
        registry.registerCommand(new AdminWeaponCommand(
            plugin.getPlayerStatsManager(),
            plugin.getWeaponUpgradeService()
        ));
        registry.registerCommand(new OpenInventoryCommand());
    }
}
