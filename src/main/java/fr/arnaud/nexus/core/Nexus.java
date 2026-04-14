package fr.arnaud.nexus.core;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.arnaud.nexus.debug.PacketDiagnostic;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeExecutionSystem;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeTriggerSystem;
import fr.arnaud.nexus.feature.movement.PlayerDashSystem;
import fr.arnaud.nexus.feature.ressource.EssenceDustManager;
import fr.arnaud.nexus.input.PlayerInputListener;
import fr.arnaud.nexus.input.VoxelTargetResolver;
import fr.arnaud.nexus.input.hover.PlayerMouseMotionListener;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDamageInterceptor;
import fr.arnaud.nexus.item.weapon.generator.EnchantmentPoolService;
import fr.arnaud.nexus.item.weapon.generator.WeaponGenerator;
import fr.arnaud.nexus.item.weapon.level.WeaponUpgradeService;
import fr.arnaud.nexus.item.weapon.system.StatIndexResolver;
import fr.arnaud.nexus.item.weapon.system.WeaponEquipSystem;
import fr.arnaud.nexus.level.LevelManager;
import fr.arnaud.nexus.level.NexusWorldLoadSystem;
import fr.arnaud.nexus.spawner.MobSpawnerManager;
import fr.arnaud.nexus.ui.inventory.InventoryPacketInterceptor;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public final class Nexus extends JavaPlugin {

    private static Nexus instance;

    private final PacketDiagnostic packetDiagnostic = new PacketDiagnostic();
    private final InventoryPacketInterceptor inventoryPacketInterceptor = new InventoryPacketInterceptor();

    private final EssenceDustManager essenceDustManager = new EssenceDustManager();

    private final VoxelTargetResolver voxelTargetResolver = new VoxelTargetResolver();
    private final PlayerDashSystem playerDashSystem = new PlayerDashSystem();
    private final PlayerInputListener playerInputListener = new PlayerInputListener(playerDashSystem, voxelTargetResolver);
    private final PlayerMouseMotionListener playerMouseMotionListener = new PlayerMouseMotionListener(voxelTargetResolver);

    private final SwitchStrikeTriggerSystem switchStrikeTriggerSystem = new SwitchStrikeTriggerSystem();
    private final SwitchStrikeExecutionSystem switchStrikeExecutionSystem = new SwitchStrikeExecutionSystem();

    private final NexusWorldLoadSystem nexusWorldLoadSystem = new NexusWorldLoadSystem();
    private final LevelManager levelManager = new LevelManager();
    private final MobSpawnerManager mobSpawnerManager = new MobSpawnerManager();

    private final EnchantmentPoolService enchantmentPoolService = new EnchantmentPoolService();
    private final EnchantmentDamageInterceptor enchantmentDamageInterceptor = new EnchantmentDamageInterceptor();

    private final StatIndexResolver statIndexResolver = new StatIndexResolver();
    private final WeaponEquipSystem weaponEquipSystem = new WeaponEquipSystem(statIndexResolver);
    private final WeaponGenerator weaponGenerator = new WeaponGenerator(enchantmentPoolService);
    private final WeaponUpgradeService weaponUpgradeService = new WeaponUpgradeService(essenceDustManager);

    public Nexus(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        new NexusInitializer(this).init();
        getLogger().at(Level.INFO).log(I18n.t("plugin.loaded"));
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log(I18n.t("plugin.started"));
    }

    @Override
    protected void shutdown() {
        inventoryPacketInterceptor.unregister();
        getLogger().at(Level.INFO).log(I18n.t("plugin.stopped"));
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

    public EssenceDustManager getEssenceDustManager() {
        return essenceDustManager;
    }

    public VoxelTargetResolver getVoxelTargetResolver() {
        return voxelTargetResolver;
    }

    public PlayerDashSystem getPlayerDashSystem() {
        return playerDashSystem;
    }

    public PlayerInputListener getPlayerInputListener() {
        return playerInputListener;
    }

    public PlayerMouseMotionListener getPlayerMouseMotionListener() {
        return playerMouseMotionListener;
    }

    public SwitchStrikeTriggerSystem getSwitchStrikeTriggerSystem() {
        return switchStrikeTriggerSystem;
    }

    public SwitchStrikeExecutionSystem getSwitchStrikeExecutionSystem() {
        return switchStrikeExecutionSystem;
    }

    public NexusWorldLoadSystem getNexusWorldLoadSystem() {
        return nexusWorldLoadSystem;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public MobSpawnerManager getMobSpawnerManager() {
        return mobSpawnerManager;
    }

    public EnchantmentPoolService getEnchantmentPoolService() {
        return enchantmentPoolService;
    }

    public EnchantmentDamageInterceptor getEnchantmentDamageInterceptor() {
        return enchantmentDamageInterceptor;
    }

    public StatIndexResolver getStatIndexResolver() {
        return statIndexResolver;
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
}
