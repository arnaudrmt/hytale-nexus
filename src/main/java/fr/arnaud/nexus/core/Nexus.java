package fr.arnaud.nexus.core;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.arnaud.nexus.ability.CoreAbilityRouter;
import fr.arnaud.nexus.ability.impl.DashAbility;
import fr.arnaud.nexus.ability.impl.SwitchStrikeAbility;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.input.PlayerInputListener;
import fr.arnaud.nexus.input.VoxelTargetResolver;
import fr.arnaud.nexus.input.hover.PlayerMouseMotionListener;
import fr.arnaud.nexus.item.weapon.generator.EnchantmentPoolService;
import fr.arnaud.nexus.item.weapon.generator.WeaponGenerator;
import fr.arnaud.nexus.item.weapon.level.WeaponUpgradeService;
import fr.arnaud.nexus.item.weapon.system.WeaponAbilityGuard;
import fr.arnaud.nexus.item.weapon.system.WeaponEquipSystem;
import fr.arnaud.nexus.level.LevelManager;
import fr.arnaud.nexus.level.NewRunService;
import fr.arnaud.nexus.level.NexusWorldLoadSystem;
import fr.arnaud.nexus.spawner.ChestManager;
import fr.arnaud.nexus.spawner.MobSpawnerManager;
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

    /**
     * Diagnostics
     */
    private final PacketDiagnostic packetDiagnostic = new PacketDiagnostic();
    private final InventoryPacketInterceptor inventoryPacketInterceptor = new InventoryPacketInterceptor();

    /**
     * Player Stats
     */
    private final PlayerStatsManager playerStatsManager = new PlayerStatsManager();

    /**
     * Input
     */
    private final VoxelTargetResolver voxelTargetResolver = new VoxelTargetResolver();
    private final DashAbility dashAbility = new DashAbility();
    private final SwitchStrikeAbility switchStrikeAbility = new SwitchStrikeAbility();
    private final CoreAbilityRouter coreAbilityRouter = new CoreAbilityRouter(dashAbility);
    private final PlayerInputListener playerInputListener = new PlayerInputListener(coreAbilityRouter);
    private final PlayerMouseMotionListener playerMouseMotionListener = new PlayerMouseMotionListener(voxelTargetResolver);

    /**
     * World & Level
     */
    private final NexusWorldLoadSystem nexusWorldLoadSystem = new NexusWorldLoadSystem();
    private final LevelManager levelManager = new LevelManager();
    private final NewRunService newRunService = new NewRunService();

    /**
     * Spawner
     */
    private final MobSpawnerManager mobSpawnerManager = new MobSpawnerManager(new ChestManager());
    private final WaveBarStateProvider waveBarStateProvider = new WaveBarStateProvider();

    /**
     * Weapon
     */
    private final EnchantmentPoolService enchantmentPoolService = new EnchantmentPoolService();
    private final WeaponEquipSystem weaponEquipSystem = new WeaponEquipSystem();
    private final WeaponGenerator weaponGenerator = new WeaponGenerator(enchantmentPoolService);
    private final WeaponUpgradeService weaponUpgradeService = new WeaponUpgradeService(playerStatsManager);
    private final WeaponAbilityGuard weaponAbilityGuard = new WeaponAbilityGuard();

    /**
     * Tutorial
     */
    private final TutorialInterceptor tutorialInterceptor = new TutorialInterceptor();
    private final TutorialManager tutorialManager = new TutorialManager();

    /**
     * UI
     */
    private final NexusHudSystem nexusHudSystem = new NexusHudSystem();
    private final WaveBarSystem waveBarSystem = new WaveBarSystem();


    public Nexus(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        HytaleLogger.getLogger().at(Level.INFO).log(Message.translation("nexus.plugin.loaded").getRawText());
        new NexusInitializer(this).initialize();
    }

    @Override
    protected void start() {
        HytaleLogger.getLogger().at(Level.INFO).log(Message.translation("nexus.plugin.started").getRawText());
    }

    @Override
    protected void shutdown() {
        HytaleLogger.getLogger().at(Level.INFO).log(Message.translation("nexus.plugin.stopped").getRawText());
        inventoryPacketInterceptor.unregister();
    }

    public static Nexus get() {
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

    public NexusWorldLoadSystem getNexusWorldLoadSystem() {
        return nexusWorldLoadSystem;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public NewRunService getNewRunService() {
        return newRunService;
    }

    public MobSpawnerManager getMobSpawnerManager() {
        return mobSpawnerManager;
    }

    public WaveBarStateProvider getWaveBarStateProvider() {
        return waveBarStateProvider;
    }

    public EnchantmentPoolService getEnchantmentPoolService() {
        return enchantmentPoolService;
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

    public WeaponAbilityGuard getWeaponAbilityGuard() {
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
