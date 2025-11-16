package com.mrrezik.itnt.listeners;

import com.mrrezik.itnt.itnt;
import com.mrrezik.itnt.managers.ConfigManager;
import com.mrrezik.itnt.managers.TNTManager;
import com.mrrezik.itnt.objects.CustomTNT;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent; // <-- ДОБАВЛЕНО
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import java.util.List;

/**
 * Обрабатывает все взаимодействия игроков с кастомным ТНТ.
 */
public class TNTListener implements Listener {

    private final itnt plugin;
    private final ConfigManager configManager;
    private final TNTManager tntManager;

    public TNTListener(itnt plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.tntManager = plugin.getTntManager();
    }

    // --- НОВАЯ ЛОГИКА: Выпадение кастомного ТНТ при разрушении ---

    /**
     * Срабатывает при разрушении блока ТНТ.
     * Отменяет ванильное выпадение и роняет кастомный предмет.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();

        // 1. Проверяем, это ли блок ТНТ
        if (brokenBlock.getType() != Material.TNT) {
            return;
        }

        // 2. Проверяем наличие нашей Metadata
        if (brokenBlock.hasMetadata("itnt-id")) {
            // Получаем ID
            List<MetadataValue> metadataValues = brokenBlock.getMetadata("itnt-id");
            if (metadataValues.isEmpty()) {
                return;
            }
            String tntId = metadataValues.get(0).asString();

            // 3. Получаем конфиг и предмет
            CustomTNT tntConfig = configManager.getTNTType(tntId);
            if (tntConfig == null) {
                plugin.getLogger().warning("Сломан ТНТ с неизвестным ID: " + tntId);
                return;
            }

            ItemStack customTNTItem = configManager.getTNTItem(tntConfig, 1);

            // 4. Отменяем стандартное выпадение блока
            event.setDropItems(false);

            // 5. Роняем наш кастомный предмет
            brokenBlock.getWorld().dropItemNaturally(brokenBlock.getLocation().add(0.5, 0.5, 0.5), customTNTItem);

            // 6. Удаляем метаданные
            brokenBlock.removeMetadata("itnt-id", plugin);
        }
        // Если метаданных нет, блок считается ванильным ТНТ (или был поставлен другим плагином)
        // и ломается как обычно.
    }


    /**
     * Срабатывает при установке блока ТНТ.
     * Отвечает ТОЛЬКО за ТНТ с auto-ignite: false
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();
        Player player = event.getPlayer();

        // 1. Проверяем, наш ли это ТНТ
        String tntId = configManager.getTNTIdFromItem(itemInHand);
        if (tntId == null) {
            return;
        }

        // 2. Получаем конфиг
        CustomTNT tntConfig = configManager.getTNTType(tntId);
        if (tntConfig == null) {
            plugin.getLogger().warning("Игрок " + player.getName() + " попытался поставить ТНТ с неизвестным ID: " + tntId);
            return;
        }

        // 3. Проверяем права
        if (!player.hasPermission("itnt.place." + tntId) && !player.hasPermission("itnt.place.*")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            event.setCancelled(true);
            return;
        }

        // 4. Проверяем флаг "auto-ignite"
        if (tntConfig.isAutoIgnite()) {
            // Логикой auto-ignite занимается onAutoIgniteInteract(PlayerInteractEvent)
            event.setCancelled(true);
        } else {
            // Если auto-ignite = false, ставим блок, но добавляем ему Metadata для отслеживания
            event.getBlockPlaced().setMetadata("itnt-id",
                    new FixedMetadataValue(plugin, tntId));
        }
    }

    /**
     * Срабатывает, когда игрок кликает блоком ТНТ с auto-ignite: true.
     * (Перехватывает клики по сундукам, кнопкам и т.д.)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAutoIgniteInteract(PlayerInteractEvent event) {
        // Нас интересует только правый клик по блоку
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Только основная рука
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack itemInHand = event.getItem();
        String tntId = configManager.getTNTIdFromItem(itemInHand);
        if (tntId == null) {
            return;
        }

        CustomTNT tntConfig = configManager.getTNTType(tntId);
        if (tntConfig == null || !tntConfig.isAutoIgnite()) {
            // Это не auto-ignite ТНТ, onBlockPlace разберется с ним.
            return;
        }

        // --- Это наш auto-ignite ТНТ ---

        // 1. Отменяем событие (e.g. открытие сундука)
        event.setCancelled(true);

        // 2. Проверяем права
        Player player = event.getPlayer();
        if (!player.hasPermission("itnt.place." + tntId) && !player.hasPermission("itnt.place.*")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        // 3. Определяем локацию для спавна ТНТ
        Location loc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();

        // 4. Проверяем, можно ли тут разместить (не внутри другого блока)
        if (!loc.getBlock().isReplaceable()) {
            // Место занято, ничего не делаем
            return;
        }

        // 5. Зажигаем ТНТ (primeTNT сам удалит блок и создаст сущность)
        tntManager.primeTNT(loc, tntConfig, player);

        // 6. Забираем 1 предмет из рук (если не в креативе)
        if (player.getGameMode() != GameMode.CREATIVE) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        }
    }


    /**
     * Срабатывает при поджиге ТНТ зажигалкой.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTIgnite(PlayerInteractEvent event) {
        // Нас интересует только правый клик по блоку основной рукой
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.TNT) {
            return;
        }

        // Проверяем, это ли поджиг
        ItemStack item = event.getItem();
        Material itemType = (item != null) ? item.getType() : null;

        if (itemType != Material.FLINT_AND_STEEL && itemType != Material.FIRE_CHARGE) {
            return;
        }

        // Проверяем наши Metadata
        if (clickedBlock.hasMetadata("itnt-id")) {
            String tntId = clickedBlock.getMetadata("itnt-id").get(0).asString();
            CustomTNT tntConfig = configManager.getTNTType(tntId);

            if (tntConfig != null) {
                // Отменяем ванильный поджиг
                event.setCancelled(true);

                // Убираем Metadata, т.к. блок будет удален в primeTNT
                clickedBlock.removeMetadata("itnt-id", plugin);

                // Запускаем наш таймер (primeTNT сам удалит блок и создаст сущность)
                tntManager.primeTNT(clickedBlock.getLocation(), tntConfig, event.getPlayer());

                // Наносим урон зажигалке / расходуем огненный шар (если не креатив)
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    if (itemType == Material.FLINT_AND_STEEL) {
                        damageFlintAndSteel(item);
                    } else {
                        // Для Fire Charge (огненный шар)
                        item.setAmount(item.getAmount() - 1);
                    }
                }
            }
        }
    }

    /**
     * Наносит 1 урон зажигалке.
     */
    private void damageFlintAndSteel(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            damageable.setDamage(damageable.getDamage() + 1);
            item.setItemMeta(meta);

            // Проверяем, не сломалась ли зажигалка
            if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                item.setAmount(0);
            }
        }
    }


    /**
     * Отменяет урон от взрыва, если у ТНТ был флаг entity-damage: false.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {

            if (tntManager.isNoDamageExplosion(event.getEntity().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
}