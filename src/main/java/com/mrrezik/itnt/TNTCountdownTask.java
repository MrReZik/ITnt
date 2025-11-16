package com.mrrezik.itnt.tasks;

import com.mrrezik.itnt.itnt;
import com.mrrezik.itnt.managers.ConfigManager;
import com.mrrezik.itnt.managers.HologramManager;
import com.mrrezik.itnt.objects.ActiveTNT;
import com.mrrezik.itnt.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class TNTCountdownTask extends BukkitRunnable {

    private final itnt plugin;
    private final ActiveTNT activeTNT;
    private final ConfigManager configManager;
    private final HologramManager hologramManager;

    public TNTCountdownTask(itnt plugin, ActiveTNT activeTNT) {
        this.plugin = plugin;
        this.activeTNT = activeTNT;
        this.configManager = plugin.getConfigManager();
        this.hologramManager = plugin.getHologramManager();
    }

    @Override
    public void run() {
        long elapsedMillis = System.currentTimeMillis() - activeTNT.getStartTime();
        long totalMillis = activeTNT.getFuseTicks() * 50L; // 50ms per tick
        double remainingSeconds = (totalMillis - elapsedMillis) / 1000.0;

        // 1. Проверяем, взорвался ли ТНТ
        if (remainingSeconds <= 0) {
            // Время вышло -> Взрываем!
            plugin.getTntManager().createExplosion(activeTNT);
            this.cancel();
            return;
        }

        // 2. Проверяем, не была ли сущность ТНТ удалена/сломана
        Entity tntEntity = activeTNT.getLocation().getWorld().getEntity(activeTNT.getEntityId());

        // Если сущности нет (она была сломана или удалена)
        if (tntEntity == null || tntEntity.isDead()) {
            // Если сущности нет, просто убираем голограмму и отменяем таймер
            hologramManager.deleteHologram(activeTNT.getTrackingId());
            plugin.getTntManager().getActiveTNTs().remove(activeTNT.getTrackingId());
            this.cancel();
            return;
        }


        // 3. Обновляем голограмму (если включена)
        if (configManager.isHologramEnabled()) {
            // Проверяем, жива ли голограмма
            if (!hologramManager.isHologramAlive(activeTNT.getTrackingId())) {
                plugin.getTntManager().getActiveTNTs().remove(activeTNT.getTrackingId());
                this.cancel();
                return;
            }

            // *** ОБНОВЛЕНИЕ МЕСТОПОЛОЖЕНИЯ ГОЛОГРАММЫ (ДВИЖЕНИЕ) ***
            Location entityLoc = tntEntity.getLocation();
            // В moveHologram мы передаем локацию сущности, а менеджер добавляет смещение (offset)
            hologramManager.moveHologram(activeTNT.getTrackingId(), entityLoc);
            // ********************************************

            String time = String.format("%.1f", Math.max(0.0, remainingSeconds));
            String name = configManager.getHologramFormat()
                    .replace("%name%", activeTNT.getConfig().getDisplayName())
                    .replace("%time%", time);

            hologramManager.updateHologram(activeTNT.getTrackingId(), Utils.color(name));
        }
    }
}