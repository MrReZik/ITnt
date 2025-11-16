package com.mrrezik.itnt.managers;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.mrrezik.itnt.itnt;
import com.mrrezik.itnt.utils.Utils;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final itnt plugin;
    private final ConfigManager configManager;
    private Provider provider = Provider.NONE;

    // Хранит активные голограммы.
    // Ключ - наш UUID, Значение - объект голограммы (ArmorStand, Hologram (HD), Hologram (DH))
    private final Map<UUID, Object> activeHolograms = new ConcurrentHashMap<>();

    private enum Provider {
        NONE,
        ARMORSTAND,
        HOLOGRAPHIC_DISPLAYS,
        DECENT_HOLOGRAMS
    }

    public HologramManager(itnt plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    /**
     * Инициализирует менеджер голограмм: определяет доступный провайдер.
     */
    public void init() {
        if (!configManager.isHologramEnabled()) {
            provider = Provider.NONE;
            plugin.getLogger().info("Holograms are disabled in config.yml.");
            return;
        }

        String configuredProvider = configManager.getHologramProvider();

        if (configuredProvider.equalsIgnoreCase("DecentHolograms") && Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            provider = Provider.DECENT_HOLOGRAMS;
            plugin.getLogger().info("DecentHolograms detected and enabled as hologram provider.");
        } else if (configuredProvider.equalsIgnoreCase("HolographicDisplays") && Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            provider = Provider.HOLOGRAPHIC_DISPLAYS;
            plugin.getLogger().info("HolographicDisplays detected and enabled as hologram provider.");
        } else if (configuredProvider.equalsIgnoreCase("ArmorStand")) {
            provider = Provider.ARMORSTAND;
            plugin.getLogger().info("Using vanilla ArmorStand as hologram provider.");
        } else {
            provider = Provider.NONE;
            plugin.getLogger().warning("No valid hologram provider found or configured. Holograms disabled.");
        }
    }

    /**
     * Создает новую голограмму.
     */
    public void createHologram(Location location, String text, UUID trackingId) {
        if (provider == Provider.NONE) return;

        try {
            // Рассчитываем конечную локацию, куда будет установлена голограмма
            Location holoLoc = location.clone().add(0, configManager.getHologramOffset(), 0);

            switch (provider) {
                case ARMORSTAND:
                    // Создаем ванильный ArmorStand
                    ArmorStand as = (ArmorStand) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
                    as.setCustomName(text);
                    as.setCustomNameVisible(true);
                    as.setGravity(false);
                    as.setMarker(true);
                    as.setVisible(false);
                    activeHolograms.put(trackingId, as);
                    break;
                case HOLOGRAPHIC_DISPLAYS:
                    // Создаем HolographicDisplays голограмму
                    Hologram holoHD = HologramsAPI.createHologram(plugin, holoLoc);
                    holoHD.appendTextLine(text);
                    activeHolograms.put(trackingId, holoHD);
                    break;
                case DECENT_HOLOGRAMS:
                    // Создаем DecentHolograms голограмму
                    eu.decentsoftware.holograms.api.holograms.Hologram holoDH =
                            DHAPI.createHologram("itnt-" + trackingId.toString(), holoLoc, List.of(text));
                    activeHolograms.put(trackingId, holoDH);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create hologram: " + e.getMessage());
        }
    }

    /**
     * Обновляет текст существующей голограммы.
     */
    public void updateHologram(UUID trackingId, String newText) {
        if (provider == Provider.NONE) return;

        Object holoObj = activeHolograms.get(trackingId);
        if (holoObj == null) return;

        try {
            switch (provider) {
                case ARMORSTAND:
                    ((ArmorStand) holoObj).setCustomName(newText);
                    break;
                case HOLOGRAPHIC_DISPLAYS:
                    Hologram holoHD = (Hologram) holoObj;
                    // *** ИСПРАВЛЕНИЕ: Используем removeLine(0) и appendTextLine для обновления текста ***
                    if (holoHD.size() > 0) {
                        holoHD.removeLine(0);
                    }
                    holoHD.appendTextLine(newText);
                    break;
                case DECENT_HOLOGRAMS:
                    DHAPI.setHologramLines((eu.decentsoftware.holograms.api.holograms.Hologram) holoObj, List.of(newText));
                    break;
            }
        } catch (Exception e) {
            // Игнорируем, если голограмма была удалена
        }
    }

    /**
     * Перемещает голограмму за движущейся сущностью.
     * @param trackingId Наш UUID
     * @param entityLocation Локация сущности ТНТ
     */
    public void moveHologram(UUID trackingId, Location entityLocation) {
        if (provider == Provider.NONE) return;

        Object holoObj = activeHolograms.get(trackingId);
        if (holoObj == null) return;

        // Добавляем смещение (offset) к локации сущности
        Location newLocation = entityLocation.clone().add(0, configManager.getHologramOffset(), 0);

        try {
            switch (provider) {
                case ARMORSTAND:
                    // Телепортируем ванильный ArmorStand
                    ((ArmorStand) holoObj).teleport(newLocation);
                    break;
                case HOLOGRAPHIC_DISPLAYS:
                    // Телепортируем HolographicDisplays голограмму
                    ((Hologram) holoObj).teleport(newLocation);
                    break;
                case DECENT_HOLOGRAMS:
                    // Перемещаем DecentHolograms голограмму
                    DHAPI.moveHologram("itnt-" + trackingId.toString(), newLocation);
                    break;
            }
        } catch (Exception e) {
            // Игнорируем ошибки, если объект был удален
        }
    }

    /**
     * Проверяет, жива ли голограмма
     * @param trackingId Наш UUID
     */
    public boolean isHologramAlive(UUID trackingId) {
        if (provider == Provider.NONE) return false;

        Object holoObj = activeHolograms.get(trackingId);
        if (holoObj == null) return false;

        try {
            switch (provider) {
                case ARMORSTAND:
                    return !((ArmorStand) holoObj).isDead();
                case HOLOGRAPHIC_DISPLAYS:
                    // Проверяем, существует ли еще голограмма в API
                    return HologramsAPI.getHolograms(plugin).stream()
                            .anyMatch(h -> h.getLocation().equals(((Hologram) holoObj).getLocation()));
                case DECENT_HOLOGRAMS:
                    // Проверяем, существует ли объект
                    return DHAPI.getHologram("itnt-" + trackingId.toString()) != null;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * Удаляет голограмму
     * @param trackingId Наш UUID
     */
    public void deleteHologram(UUID trackingId) {
        if (provider == Provider.NONE) return;

        Object holoObj = activeHolograms.remove(trackingId);
        if (holoObj == null) return;

        try {
            switch (provider) {
                case ARMORSTAND:
                    ((ArmorStand) holoObj).remove();
                    break;
                case HOLOGRAPHIC_DISPLAYS:
                    ((Hologram) holoObj).delete();
                    break;
                case DECENT_HOLOGRAMS:
                    ((eu.decentsoftware.holograms.api.holograms.Hologram) holoObj).delete();
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete hologram: " + e.getMessage());
        }
    }

    /**
     * Очищает все активные голограммы (при перезагрузке/выключении)
     */
    public void cleanupAll() {
        if (provider == Provider.NONE) return;

        for (UUID id : activeHolograms.keySet()) {
            deleteHologram(id); // Используем deleteHologram для корректной очистки
        }
        activeHolograms.clear();
    }
}