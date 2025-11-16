package com.mrrezik.itnt.managers;

import com.mrrezik.itnt.itnt;
import com.mrrezik.itnt.objects.CustomTNT;
import com.mrrezik.itnt.utils.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Управляет конфигурацией плагина (config.yml), кеширует типы ТНТ и сообщения.
 */
public class ConfigManager {

    private final itnt plugin;
    private FileConfiguration config;

    // Кеш для быстрого доступа
    private final Map<String, CustomTNT> tntCache = new HashMap<>();
    private final Map<String, String> tntAliasMap = new HashMap<>(); // <alias, tnt-id>
    private final Map<String, String> messages = new HashMap<>();

    // Настройки голограмм
    private boolean hologramEnabled;
    private String hologramFormat;
    private double hologramOffset;
    private String hologramProvider;

    public static final NamespacedKey TNT_ID_KEY;

    // Статический инициализатор для ключа NBT
    static {
        TNT_ID_KEY = new NamespacedKey(itnt.getInstance(), "tnt-id");
    }

    public ConfigManager(itnt plugin) {
        this.plugin = plugin;
    }

    /**
     * Загружает и кеширует все значения из config.yml.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Очищаем кеш перед загрузкой
        tntCache.clear();
        tntAliasMap.clear();
        messages.clear();

        // Загрузка настроек голограмм
        hologramEnabled = config.getBoolean("hologram.enabled", true);
        hologramFormat = config.getString("hologram.format", "&#FF6347%name% &f- &e%time%s");
        hologramOffset = config.getDouble("hologram.offset-y", 0.8);
        hologramProvider = config.getString("hologram.provider", "ArmorStand");

        // Загрузка сообщений
        loadMessages();

        // Загрузка типов ТНТ
        loadTNTTypes();
    }

    private void loadMessages() {
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messages.put(key, Utils.color(msgSection.getString(key)));
            }
        }
    }

    private void loadTNTTypes() {
        ConfigurationSection tntSection = config.getConfigurationSection("tnt");
        if (tntSection == null) {
            plugin.getLogger().warning("Секция 'tnt:' не найдена в config.yml!");
            return;
        }

        for (String id : tntSection.getKeys(false)) {
            ConfigurationSection cs = tntSection.getConfigurationSection(id);
            if (cs == null) continue;

            CustomTNT tnt = new CustomTNT(
                    id,
                    cs.getString("display-name"),
                    cs.getStringList("lore"),
                    cs.getInt("fuse-time", 4),
                    cs.getBoolean("auto-ignite", false),
                    (float) cs.getDouble("power", 4.0),
                    cs.getBoolean("block-damage", true),
                    cs.getBoolean("entity-damage", true),
                    cs.getBoolean("explode-in-water", false),
                    cs.getBoolean("break-obsidian", false),
                    cs.getStringList("disabled-worlds")
            );

            // Кешируем ТНТ
            tntCache.put(id.toLowerCase(), tnt);

            // НОВОЕ: Загружаем и кешируем алиасы
            List<String> aliases = cs.getStringList("aliases");
            for (String alias : aliases) {
                if (tntAliasMap.containsKey(alias.toLowerCase())) {
                    plugin.getLogger().warning("Дубликат алиаса '" + alias + "'! Он будет проигнорирован.");
                } else if (tntCache.containsKey(alias.toLowerCase())) {
                    plugin.getLogger().warning("Алиас '" + alias + "' конфликтует с ID другого ТНТ! Он будет проигнорирован.");
                } else {
                    tntAliasMap.put(alias.toLowerCase(), id.toLowerCase());
                }
            }
        }
    }

    /**
     * Перезагружает config.yml
     */
    public void reloadConfig() {
        loadConfig();
    }

    // --- Getters ---

    /**
     * Находит CustomTNT по его ID или алиасу.
     * @param id ID или алиас ТНТ (регистронезависимо)
     * @return CustomTNT или null, если не найден
     */
    public CustomTNT getTNTType(String id) {
        String normalizedId = id.toLowerCase();
        CustomTNT tnt = tntCache.get(normalizedId);
        if (tnt == null) {
            // Если не нашли по ID, ищем в алиасах
            String mainId = tntAliasMap.get(normalizedId);
            if (mainId != null) {
                tnt = tntCache.get(mainId);
            }
        }
        return tnt;
    }

    /**
     * @return Коллекция всех зарегистрированных CustomTNT (только оригиналы, без алиасов).
     */
    public Collection<CustomTNT> getAllTNTTypes() {
        return tntCache.values();
    }

    /**
     * @return Множество всех ID и Алиасов для автодополнения команд.
     */
    public Set<String> getTNTTypeKeys() {
        Set<String> keys = new HashSet<>(tntCache.keySet());
        keys.addAll(tntAliasMap.keySet());
        return keys;
    }

    /**
     * @param key Ключ сообщения
     * @return Отформатированное сообщение с префиксом
     */
    public String getMessage(String key) {
        String prefix = messages.getOrDefault("prefix", "");
        return prefix + messages.getOrDefault(key, "&cСообщение не найдено: " + key);
    }

    /**
     * @param key Ключ сообщения
     * @return "Сырое" отформатированное сообщение без префикса
     */
    public String getRawMessage(String key) {
        return messages.getOrDefault(key, "&cСообщение не найдено: " + key);
    }

    // --- Getters (Hologram) ---
    public boolean isHologramEnabled() { return hologramEnabled; }
    public String getHologramFormat() { return hologramFormat; }
    public double getHologramOffset() { return hologramOffset; }
    public String getHologramProvider() { return hologramProvider; }

    // --- Item Utils ---

    /**
     * Создает ItemStack для кастомного ТНТ с NBT-тегом.
     * @param tnt Конфигурация ТНТ
     * @param amount Количество
     * @return ItemStack
     */
    public ItemStack getTNTItem(CustomTNT tnt, int amount) {
        ItemStack item = new ItemStack(Material.TNT, amount);
        ItemMeta meta = item.getItemMeta();

        // ItemMeta не может быть null, если предмет только что создан
        meta.setDisplayName(Utils.color(tnt.getDisplayName()));
        meta.setLore(Utils.color(tnt.getLore()));
        meta.getPersistentDataContainer().set(TNT_ID_KEY, PersistentDataType.STRING, tnt.getId());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Проверяет, является ли предмет кастомным ТНТ, и возвращает его ID.
     * @param item Предмет для проверки
     * @return ID ТНТ или null
     */
    public String getTNTIdFromItem(ItemStack item) {
        if (item == null || item.getType() != Material.TNT || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(TNT_ID_KEY, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(TNT_ID_KEY, PersistentDataType.STRING);
        }
        return null;
    }
}