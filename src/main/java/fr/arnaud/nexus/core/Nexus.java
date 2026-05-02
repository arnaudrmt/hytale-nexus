package fr.arnaud.nexus.core;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.arnaud.nexus.ability.core.CoreAbilityRouter;
import fr.arnaud.nexus.ability.dash.DashAbility;
import fr.arnaud.nexus.ability.strike.SwitchStrikeAbility;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.input.PlayerInputListener;
import fr.arnaud.nexus.input.VoxelTargetResolver;
import fr.arnaud.nexus.input.hover.PlayerMouseMotionListener;
import fr.arnaud.nexus.item.weapon.generator.WeaponGenerator;
import fr.arnaud.nexus.item.weapon.level.WeaponUpgradeService;
import fr.arnaud.nexus.item.weapon.system.WeaponEquipSystem;
import fr.arnaud.nexus.item.weapon.system.WeaponUsageGuard;
import fr.arnaud.nexus.level.LevelTransitionService;
import fr.arnaud.nexus.level.LevelWorldService;
import fr.arnaud.nexus.level.RunStartService;
import fr.arnaud.nexus.spawner.SpawnerRegistry;
import fr.arnaud.nexus.spawner.WaveBarStateProvider;
import fr.arnaud.nexus.tutorial.TutorialInterceptor;
import fr.arnaud.nexus.tutorial.TutorialManager;
import fr.arnaud.nexus.ui.NexusHudSystem;
import fr.arnaud.nexus.ui.hud.WaveBarSystem;
import fr.arnaud.nexus.ui.inventory.InventoryPacketInterceptor;
import fr.arnaud.nexus.util.PacketDiagnostic;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public final class Nexus extends JavaPlugin {

    private static Nexus instance;

    // Diagnostics
    private final PacketDiagnostic packetDiagnostic = new PacketDiagnostic();

    // World & Level
    private final LevelWorldService levelWorldService = new LevelWorldService();
    private final LevelTransitionService levelTransitionService = new LevelTransitionService();
    private final RunStartService runStartService = new RunStartService();

    // Player
    private final PlayerStatsManager playerStatsManager = new PlayerStatsManager();

    // Input & Core Abilities
    private final DashAbility dashAbility = new DashAbility();
    private final SwitchStrikeAbility switchStrikeAbility = new SwitchStrikeAbility();
    private final CoreAbilityRouter coreAbilityRouter = new CoreAbilityRouter(dashAbility, switchStrikeAbility);

    private final VoxelTargetResolver voxelTargetResolver = new VoxelTargetResolver();
    private final PlayerInputListener playerInputListener = new PlayerInputListener(coreAbilityRouter);
    private final PlayerMouseMotionListener playerMouseMotionListener = new PlayerMouseMotionListener(voxelTargetResolver);

    // Inventory
    private final InventoryPacketInterceptor inventoryPacketInterceptor = new InventoryPacketInterceptor();

    // UI
    private final NexusHudSystem nexusHudSystem = new NexusHudSystem();
    private final WaveBarSystem waveBarSystem = new WaveBarSystem();

    // Spawner
    private final WaveBarStateProvider waveBarStateProvider = new WaveBarStateProvider();
    private final SpawnerRegistry spawnerRegistry = new SpawnerRegistry(levelTransitionService, waveBarStateProvider);

    // Weapon
    private final WeaponGenerator weaponGenerator = new WeaponGenerator();
    private final WeaponUpgradeService weaponUpgradeService = new WeaponUpgradeService(playerStatsManager);
    private final WeaponEquipSystem weaponEquipSystem = new WeaponEquipSystem();
    private final WeaponUsageGuard weaponAbilityGuard = new WeaponUsageGuard();

    // Tutorial
    private final TutorialInterceptor tutorialInterceptor = new TutorialInterceptor();
    private final TutorialManager tutorialManager = new TutorialManager();

    public Nexus(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        HytaleLogger.getLogger().at(Level.INFO).log("Project Nexus: Loaded.");
        new NexusInitializer(this).initialize();
    }

    @Override
    protected void start() {
        HytaleLogger.getLogger().at(Level.INFO).log("Project Nexus: Started.");
    }

    @Override
    protected void shutdown() {
        HytaleLogger.getLogger().at(Level.INFO).log("Project Nexus: Stopped.");
        inventoryPacketInterceptor.unregister();
    }

    public static Nexus getInstance() {
        return instance;
    }

    public PacketDiagnostic getPacketDiagnostic() {
        return packetDiagnostic;
    }

    public InventoryPacketInterceptor getInventoryPacketInterceptor() {
        return inventoryPacketInterceptor;
    }

    public PlayerStatsManager getPlayerStatsManager() {
        return playerStatsManager;
    }

    public VoxelTargetResolver getVoxelTargetResolver() {
        return voxelTargetResolver;
    }

    public DashAbility getDashAbility() {
        return dashAbility;
    }

    public SwitchStrikeAbility getSwitchStrikeAbility() {
        return switchStrikeAbility;
    }

    public CoreAbilityRouter getCoreAbilityRouter() {
        return coreAbilityRouter;
    }

    public PlayerInputListener getPlayerInputListener() {
        return playerInputListener;
    }

    public PlayerMouseMotionListener getPlayerMouseMotionListener() {
        return playerMouseMotionListener;
    }

    public LevelWorldService getLevelWorldService() {
        return levelWorldService;
    }

    public LevelTransitionService getLevelTransitionService() {
        return levelTransitionService;
    }

    public RunStartService getRunStartService() {
        return runStartService;
    }

    public SpawnerRegistry getSpawnerRegistry() {
        return spawnerRegistry;
    }

    public WaveBarStateProvider getWaveBarStateProvider() {
        return waveBarStateProvider;
    }

    public WeaponEquipSystem getWeaponEquipSystem() {
        return weaponEquipSystem;
    }

    public WeaponGenerator getWeaponGenerator() {
        return weaponGenerator;
    }

    public WeaponUpgradeService getWeaponUpgradeService() {
        return weaponUpgradeService;
    }

    public WeaponUsageGuard getWeaponAbilityGuard() {
        return weaponAbilityGuard;
    }

    public TutorialInterceptor getTutorialInterceptor() {
        return tutorialInterceptor;
    }

    public TutorialManager getTutorialManager() {
        return tutorialManager;
    }

    public NexusHudSystem getNexusHudSystem() {
        return nexusHudSystem;
    }

    public WaveBarSystem getWaveBarSystem() {
        return waveBarSystem;
    }
}
