package fr.arnaud.nexus.core;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.camera.*;
import fr.arnaud.nexus.command.*;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.event.RunCompletedEvent;
import fr.arnaud.nexus.event.RunCompletedHandler;
import fr.arnaud.nexus.feature.combat.HeadLockComponent;
import fr.arnaud.nexus.feature.combat.HeadTrackingSystem;
import fr.arnaud.nexus.feature.combat.PlayerBodyStateComponent;
import fr.arnaud.nexus.feature.combat.PlayerLocomotionSystem;
import fr.arnaud.nexus.feature.combat.strike.*;
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
import fr.arnaud.nexus.item.weapon.system.WeaponAbilityGuard;
import fr.arnaud.nexus.item.weapon.system.WeaponSwapSystem;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.session.PlayerSessionTracker;
import fr.arnaud.nexus.spawner.PlayerRespawnSystem;
import fr.arnaud.nexus.spawner.SpawnerMobDeathSystem;
import fr.arnaud.nexus.spawner.SpawnerProximitySystem;
import fr.arnaud.nexus.spawner.SpawnerTagComponent;
import fr.arnaud.nexus.system.NexusStoragePickupGuard;
import fr.arnaud.nexus.tutorial.TutorialTimerSystem;

import javax.annotation.Nullable;

public final class NexusInitializer {

    private final Nexus plugin;

    public NexusInitializer(Nexus plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        initializeLoaders();
        registerComponents();
        registerSystems();
        registerListeners();
        registerCommands();
    }

    private void initializeLoaders() {
        WeaponStatConfigLoader.load();
        EnchantmentRegistry.get().loadAllEnchantments();
        EnchantmentRegistrar.registerAll();
        plugin.getTutorialInterceptor().register();
        plugin.getTutorialManager().loadSteps();
    }

    private void registerComponents() {
        var registry = plugin.getEntityStoreRegistry();

        HeadLockComponent.setComponentType(registry.registerComponent(HeadLockComponent.class, HeadLockComponent::new));
        RunSessionComponent.setComponentType(registry.registerComponent(RunSessionComponent.class, "Nexus_RunSession", RunSessionComponent.CODEC));
        ActiveCoreComponent.setComponentType(registry.registerComponent(ActiveCoreComponent.class, "Nexus_ActiveCore", ActiveCoreComponent.CODEC));
        PlayerCameraComponent.setComponentType(registry.registerComponent(PlayerCameraComponent.class, PlayerCameraComponent::new));
        PlayerOcclusionComponent.setComponentType(registry.registerComponent(PlayerOcclusionComponent.class, PlayerOcclusionComponent::new));
        PlayerBodyStateComponent.setComponentType(registry.registerComponent(PlayerBodyStateComponent.class, PlayerBodyStateComponent::new));
        PlayerDashComponent.setComponentType(registry.registerComponent(PlayerDashComponent.class, PlayerDashComponent::new));
        PlayerCursorTargetComponent.setComponentType(registry.registerComponent(PlayerCursorTargetComponent.class, PlayerCursorTargetComponent::new));
        PlayerHoverStateComponent.setComponentType(registry.registerComponent(PlayerHoverStateComponent.class, PlayerHoverStateComponent::new));
        StrikeComponent.setComponentType(registry.registerComponent(StrikeComponent.class, StrikeComponent::new));
        LevelProgressComponent.setComponentType(registry.registerComponent(LevelProgressComponent.class, "Nexus_LevelProgress", LevelProgressComponent.CODEC));
        SpawnerTagComponent.setComponentType(registry.registerComponent(SpawnerTagComponent.class, "Nexus_SpawnerTag", SpawnerTagComponent.CODEC));
        WeaponInstanceComponent.setComponentType(registry.registerComponent(WeaponInstanceComponent.class, WeaponInstanceComponent::new));
        PlayerWeaponStateComponent.setComponentType(
            registry.registerComponent(PlayerWeaponStateComponent.class, "Nexus_PlayerWeaponState", PlayerWeaponStateComponent.CODEC)
        );
        registry.registerComponent(InventoryComponent.Storage.class, InventoryComponent.Storage::new);
        StrikePendingComponent.setComponentType(registry.registerComponent(StrikePendingComponent.class, StrikePendingComponent::new));
        StrikeSwapConfirmedComponent.setComponentType(registry.registerComponent(StrikeSwapConfirmedComponent.class, StrikeSwapConfirmedComponent::new));
        StrikeShockwavePendingComponent.setComponentType(registry.registerComponent(StrikeShockwavePendingComponent.class, StrikeShockwavePendingComponent::new));
    }

    private void registerSystems() {
        var registry = plugin.getEntityStoreRegistry();

        registry.registerSystem(buildItemDurabilityRestoreSystem());
        registry.registerSystem(buildItemDropPreventionSystem());

        registry.registerSystem(new HeadTrackingSystem());
        registry.registerSystem(new PlayerCameraSystem());
        registry.registerSystem(new CameraOcclusionSystem());
        registry.registerSystem(new PlayerLocomotionSystem());
        registry.registerSystem(plugin.getDashAbility());
        registry.registerSystem(new StrikeSystem());
        registry.registerSystem(new StrikeHitInterceptor());
        new StrikePacketInterceptor(plugin.getPlayerStatsManager());
        new StrikeWeaponSwapInterceptor();
        registry.registerSystem(new EnchantmentDamageInterceptor.OnHitSystem());
        registry.registerSystem(new EnchantmentDamageInterceptor.OnReceiveHitSystem());
        registry.registerSystem(new EnchantmentDamageInterceptor.OnKillSystem());
        registry.registerSystem(new SpawnerProximitySystem());
        registry.registerSystem(new SpawnerMobDeathSystem());
        registry.registerSystem(new PlayerWeaponInitSystem(plugin.getWeaponEquipSystem()));
        registry.registerSystem(new NexusStoragePickupGuard(EntityModule.get().getPlayerSpatialResourceType()));
        registry.registerSystem(new PlayerRespawnSystem());
        new WeaponSwapSystem(plugin.getWeaponEquipSystem());
        registry.registerSystem(new PlayerSessionTracker());
        registry.registerSystem(plugin.getWaveBarSystem());
        registry.registerSystem(new TutorialTimerSystem());
        new WeaponAbilityGuard();
        registry.registerSystem(new OcclusionCleanupSystem());
    }

    private EntityEventSystem<EntityStore, InventoryChangeEvent> buildItemDurabilityRestoreSystem() {
        return new EntityEventSystem<>(InventoryChangeEvent.class) {
            @Override
            public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                               CommandBuffer<EntityStore> cmd, InventoryChangeEvent event) {
                ItemContainer container = event.getItemContainer();
                for (short slot = 0; slot < container.getCapacity(); slot++) {
                    ItemStack stack = container.getItemStack(slot);
                    if (stack != null && !stack.isUnbreakable() && stack.getDurability() < stack.getMaxDurability()) {
                        container.setItemStackForSlot(slot, stack.withDurability(stack.getMaxDurability()));
                    }
                }
            }

            @Override
            public Query<EntityStore> getQuery() {
                return InventoryComponent.Hotbar.getComponentType();
            }
        };
    }

    private DeathSystems.OnDeathSystem buildItemDropPreventionSystem() {
        return new DeathSystems.OnDeathSystem() {
            @Nullable
            @Override
            public Query<EntityStore> getQuery() {
                return Query.any();
            }

            @Override
            public void onComponentAdded(Ref<EntityStore> ref, DeathComponent component,
                                         Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
                component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
            }
        };
    }

    private void registerListeners() {
        var events = plugin.getEventRegistry();

        events.registerGlobal(StartWorldEvent.class, plugin.getNexusWorldLoadSystem()::onWorldStart);
        events.registerGlobal(PlayerReadyEvent.class, PlayerSessionListener::onPlayerReady);
        events.register(LoadedAssetsEvent.class, EntityStatType.class, plugin.getPlayerStatsManager()::onAssetsLoaded);
        events.registerGlobal(PlayerMouseButtonEvent.class, plugin.getPlayerInputListener()::onMouseButton);
        events.registerGlobal(PlayerMouseMotionEvent.class, plugin.getPlayerMouseMotionListener()::onMouseMotion);

        plugin.getInventoryPacketInterceptor().register();

        events.registerGlobal(PlayerReadyEvent.class, e -> {
            Player player = e.getPlayer();
            plugin.getNexusHudSystem().onPlayerReady(player, () -> {
                plugin.getWaveBarSystem().onPlayerReady(player);
                plugin.getTutorialManager().onPlayerReady(player);
            });
        });

        events.registerGlobal(PlayerMouseButtonEvent.class, e -> plugin.getTutorialInterceptor().onMouseButton(e));

        HytaleServer.get().getEventBus().registerGlobal(RunCompletedEvent.class, new RunCompletedHandler());
    }

    private void registerCommands() {
        var registry = plugin.getCommandRegistry();

        registry.registerCommand(new AdminWeaponCommand(plugin.getPlayerStatsManager(), plugin.getWeaponUpgradeService()));
        registry.registerCommand(new OpenInventoryCommand());
        registry.registerCommand(new AdminCoreCommand());
        registry.registerCommand(new AdminRunCommand());
        registry.registerCommand(new TutorialCommand());
        registry.registerCommand(new OpenDashboardCommand());
    }
}
