package com.mrrezik.itnt.managers;

import com.mrrezik.itnt.itnt;
import com.mrrezik.itnt.objects.ActiveTNT;
import com.mrrezik.itnt.objects.CustomTNT;
import com.mrrezik.itnt.tasks.TNTCountdownTask;
import com.mrrezik.itnt.utils.Utils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет всеми активными (зажженными) ТНТ на сервере.
 */
public class TNTManager {

    private final itnt plugin;
    private final ConfigManager configManager;
    private final HologramManager hologramManager;

    private final Map<UUID, ActiveTNT> activeTNTs = new ConcurrentHashMap<>();
    private final Set<Location> noDamageExplosions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TNTManager(itnt plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.hologramManager = plugin.getHologramManager();
    }

    /**
     * Активирует (зажигает) кастомный ТНТ в мире.
     * @param location Локация блока
     * @param tntConfig Конфигурация ТНТ
     * @param placer Игрок, который поставил ТНТ (может быть null)
     */
    public void primeTNT(Location location, CustomTNT tntConfig, Player placer) {
        // 1. Проверка на запрещенный мир
        if (tntConfig.getDisabledWorlds().contains(location.getWorld().getName())) {
            if (placer != null) {
                placer.sendMessage(configManager.getMessage("tnt-disabled-in-this-world"));
                placer.getInventory().addItem(configManager.getTNTItem(tntConfig, 1));
            }
            return;
        }

        World world = location.getWorld();
        if (world == null) return;

        // --- НОВАЯ ЛОГИКА ---

        // 2. Проверка на воду
        if (location.getBlock().isLiquid() && !tntConfig.isExplodeInWater()) {
            location.getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);
            location.getWorld().spawnParticle(Particle.SMOKE_NORMAL, location.clone().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.0);
            location.getBlock().setType(Material.AIR); // Удаляем блок, если он был поставлен
            return;
        }

        // 3. Удаляем блок ТНТ (если он еще стоит)
        location.getBlock().setType(Material.AIR);

        // 4. Создаем сущность TNTPrimed
        TNTPrimed tntEntity = world.spawn(location.clone().add(0.5, 0.0, 0.5), TNTPrimed.class);

        // 5. Предотвращаем ванильный взрыв
        tntEntity.setFuseTicks(999999); // Длительный таймер

        // 6. Устанавливаем поджигателя
        if (placer != null) {
            tntEntity.setSource(placer);
        }

        // 7. Прикрепляем ID к сущности через Persistent Data Container (PDC)
        tntEntity.getPersistentDataContainer().set(ConfigManager.TNT_ID_KEY, PersistentDataType.STRING, tntConfig.getId());

        // 8. Создаем ActiveTNT
        UUID trackingId = UUID.randomUUID();
        long startTime = System.currentTimeMillis();
        long fuseTicks = tntConfig.getFuseTime() * 20L;

        ActiveTNT activeTNT = new ActiveTNT(trackingId, location, tntConfig, startTime, fuseTicks, tntEntity.getUniqueId());
        activeTNTs.put(trackingId, activeTNT);

        // 9. Создаем голограмму
        if (configManager.isHologramEnabled()) {
            String time = String.format("%.1f", (double) tntConfig.getFuseTime());
            String name = configManager.getHologramFormat()
                    .replace("%name%", tntConfig.getDisplayName())
                    .replace("%time%", time);

            Location holoLocation = location.clone().add(0.5, configManager.getHologramOffset(), 0.5);
            hologramManager.createHologram(holoLocation, Utils.color(name), trackingId);
        }

        // 10. Запускаем таймер
        new TNTCountdownTask(plugin, activeTNT).runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Логика кастомного взрыва (вызывается из TNTCountdownTask).
     */
    public void createExplosion(ActiveTNT activeTNT) {
        activeTNTs.remove(activeTNT.getTrackingId());

        Location loc = activeTNT.getLocation();
        CustomTNT config = activeTNT.getConfig();
        World world = loc.getWorld();
        if (world == null) return;

        // 1. Убираем сущность TNTPrimed
        Entity tntEntity = world.getEntity(activeTNT.getEntityId());
        if (tntEntity != null) {
            tntEntity.remove();
        }

        // 2. Убираем голограмму
        hologramManager.deleteHologram(activeTNT.getTrackingId());

        // 3. Эффекты взрыва
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        world.spawnParticle(Particle.EXPLOSION_HUGE, loc.clone().add(0.5, 0.5, 0.5), 1, 0, 0, 0);

        // --- Обработка флагов ---

        // 4. Флаг "Нет урона существам" (entity-damage: false)
        if (!config.isEntityDamage()) {
            addNoDamageLocation(loc);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> removeNoDamageLocation(loc), 2L);
        }

        // 5. Флаг "Урон блокам" (block-damage: true)
        boolean breakBlocks = config.isBlockDamage();
        boolean inWater = loc.getBlock().isLiquid();
        boolean customBlockDestructionNeeded = false;

        // Если ТНТ взрывается в воде и должен ломать блоки, мы делаем кастомное разрушение
        if (inWater && config.isExplodeInWater() && breakBlocks) {
            customBlockDestructionNeeded = true;
            breakBlocks = false; // Отключаем поломку блоков в ванильном взрыве
        }

        // 6. Создаем ЕДИНСТВЕННЫЙ взрыв
        world.createExplosion(loc.clone().add(0.5, 0.5, 0.5), config.getPower(), false, breakBlocks);

        // 7. Кастомная поломка блоков в воде
        if (customBlockDestructionNeeded) {
            handleWaterBlockBreaking(loc, config.getPower());
        }

        // 8. Флаг "Ломать обсидиан" (break-obsidian: true)
        if (config.isBreakObsidian()) {
            handleObsidianBreaking(loc, config.getPower());
        }
    }

    /**
     * Кастомная логика для ломания обсидиана в радиусе взрыва.
     */
    private void handleObsidianBreaking(Location center, float power) {
        int radius = (int) Math.ceil(power);
        World world = center.getWorld();
        if (world == null) return;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = center.clone().add(x, y, z);
                    // Проверяем сферический, а не кубический радиус
                    if (blockLoc.distanceSquared(center) > power * power) continue;

                    Block block = blockLoc.getBlock();
                    Material type = block.getType();

                    if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN || type == Material.ANCIENT_DEBRIS) {
                        block.setType(Material.AIR);
                        world.spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 30, 0.4, 0.4, 0.4, type.createBlockData());
                    }
                }
            }
        }
    }

    /**
     * Кастомная логика для поломки блоков под водой.
     * Эмулирует обычный взрыв, удаляя блоки с низкой прочностью (прочность меньше обсидиана).
     */
    private void handleWaterBlockBreaking(Location center, float power) {
        int radius = (int) Math.ceil(power);
        World world = center.getWorld();
        if (world == null) return;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = center.clone().add(x, y, z);
                    if (blockLoc.distanceSquared(center) > power * power) continue;

                    Block block = blockLoc.getBlock();
                    Material type = block.getType();

                    // *** ИСПРАВЛЕНИЕ: Замена type.isLiquid() на явную проверку Material.WATER/Material.LAVA ***
                    if (type.isAir() || type.getBlastResistance() > 6000.0f || type == Material.WATER || type == Material.LAVA) continue;

                    // Блок считается легко разрушаемым (прочность ниже 1200.0f)
                    if (type.getBlastResistance() < 1200.0f) {
                        block.breakNaturally(); // Роняем предмет
                    }
                }
            }
        }
    }

    private void addNoDamageLocation(Location loc) {
        noDamageExplosions.add(loc.getBlock().getLocation());
    }

    private void removeNoDamageLocation(Location loc) {
        noDamageExplosions.remove(loc.getBlock().getLocation());
    }

    public boolean isNoDamageExplosion(Location entityLocation) {
        if (noDamageExplosions.isEmpty()) return false;

        Location blockLoc = entityLocation.getBlock().getLocation();
        for (Location explosionLoc : noDamageExplosions) {
            // Проверяем мир и радиус 16 блоков (256 = 16^2)
            if (explosionLoc.getWorld().equals(blockLoc.getWorld()) && explosionLoc.distanceSquared(blockLoc) < 256) {
                return true;
            }
        }
        return false;
    }

    /**
     * Очищает все активные ТНТ и голограммы.
     */
    public void cleanupAll() {
        World world = null;
        for (ActiveTNT activeTNT : activeTNTs.values()) {
            // Пытаемся получить мир
            if (world == null && activeTNT.getLocation().getWorld() != null) {
                world = activeTNT.getLocation().getWorld();
            }

            // Удаляем сущность TNTPrimed
            if (world != null) {
                Entity tntEntity = world.getEntity(activeTNT.getEntityId());
                if (tntEntity != null) {
                    tntEntity.remove();
                }
            }
        }
        activeTNTs.clear();
        noDamageExplosions.clear();
    }

    public Map<UUID, ActiveTNT> getActiveTNTs() {
        return activeTNTs;
    }
}