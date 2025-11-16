package com.mrrezik.itnt;

import com.mrrezik.itnt.commands.TNTCommand;
import com.mrrezik.itnt.listeners.TNTListener;
import com.mrrezik.itnt.managers.ConfigManager;
import com.mrrezik.itnt.managers.HologramManager;
import com.mrrezik.itnt.managers.TNTManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Основной класс плагина iTNT.
 * Управляет жизненным циклом плагина и инициализацией менеджеров.
 */
public class itnt extends JavaPlugin {

    private static itnt instance;
    private ConfigManager configManager;
    private TNTManager tntManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Менеджер конфигурации (должен быть первым)
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 2. Менеджер голограмм (зависит от ConfigManager)
        hologramManager = new HologramManager(this);
        hologramManager.init();

        // 3. Менеджер ТНТ (зависит от ConfigManager и HologramManager)
        tntManager = new TNTManager(this);

        // Регистрация команд
        TNTCommand tntCommand = new TNTCommand(this);
        Objects.requireNonNull(getCommand("itnt")).setExecutor(tntCommand);
        Objects.requireNonNull(getCommand("itnt")).setTabCompleter(tntCommand);

        // Регистрация слушателей
        Bukkit.getPluginManager().registerEvents(new TNTListener(this), this);

        getLogger().info("iTNT by MrReZik has been enabled!");
    }

    @Override
    public void onDisable() {
        // Очищаем все активные ТНТ и голограммы при выключении
        if (tntManager != null) {
            tntManager.cleanupAll();
        }
        if (hologramManager != null) {
            hologramManager.cleanupAll();
        }
        getLogger().info("iTNT by MrReZik has been disabled.");
    }

    /**
     * Перезагружает конфигурацию и все зависимые компоненты.
     */
    public void reloadPlugin() {
        // 1. Очищаем старые данные (ТНТ и голограммы)
        if (tntManager != null) {
            tntManager.cleanupAll();
        }
        if (hologramManager != null) {
            hologramManager.cleanupAll();
        }

        // 2. Перезагружаем конфиг
        configManager.reloadConfig();

        // 3. Ре-инициализируем голограммы (т.к. провайдер мог измениться)
        hologramManager.init();
    }

    // --- Getters ---

    public static itnt getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TNTManager getTntManager() {
        return tntManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}