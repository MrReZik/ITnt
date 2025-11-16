package com.mrrezik.itnt.objects;

import java.util.List;

/**
 * Представляет конфигурацию одного типа ТНТ из config.yml.
 * POJO (Plain Old Java Object) - неизменяемый (immutable) класс.
 */
public class CustomTNT {
    private final String id;
    private final String displayName;
    private final List<String> lore;
    private final int fuseTime; // в секундах
    private final boolean autoIgnite;
    private final float power;
    private final boolean blockDamage;
    private final boolean entityDamage;
    private final boolean explodeInWater;
    private final boolean breakObsidian;
    private final List<String> disabledWorlds;

    public CustomTNT(String id, String displayName, List<String> lore, int fuseTime,
                     boolean autoIgnite, float power, boolean blockDamage,
                     boolean entityDamage, boolean explodeInWater,
                     boolean breakObsidian, List<String> disabledWorlds) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.fuseTime = fuseTime;
        this.autoIgnite = autoIgnite;
        this.power = power;
        this.blockDamage = blockDamage;
        this.entityDamage = entityDamage;
        this.explodeInWater = explodeInWater;
        this.breakObsidian = breakObsidian;
        this.disabledWorlds = disabledWorlds;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public int getFuseTime() { return fuseTime; }
    public boolean isAutoIgnite() { return autoIgnite; }
    public float getPower() { return power; }
    public boolean isBlockDamage() { return blockDamage; }
    public boolean isEntityDamage() { return entityDamage; }
    public boolean isExplodeInWater() { return explodeInWater; }
    public boolean isBreakObsidian() { return breakObsidian; }
    public List<String> getDisabledWorlds() { return disabledWorlds; }
}