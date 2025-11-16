package com.mrrezik.itnt.objects;

import org.bukkit.Location;
import java.util.UUID;

public class ActiveTNT {

    private final UUID trackingId;
    private final Location location;
    private final CustomTNT config;
    private final long startTime;
    private final long fuseTicks;
    private final UUID entityId; // <-- ДОБАВЛЕНО: UUID сущности TNTPrimed

    // Обновленный конструктор
    public ActiveTNT(UUID trackingId, Location location, CustomTNT config, long startTime, long fuseTicks, UUID entityId) { // <-- ИЗМЕНЕНО
        this.trackingId = trackingId;
        this.location = location;
        this.config = config;
        this.startTime = startTime;
        this.fuseTicks = fuseTicks;
        this.entityId = entityId; // <-- ДОБАВЛЕНО
    }

    public UUID getTrackingId() { return trackingId; }
    public Location getLocation() { return location; }
    public CustomTNT getConfig() { return config; }
    public long getStartTime() { return startTime; }
    public long getFuseTicks() { return fuseTicks; }
    public UUID getEntityId() { return entityId; } // <-- ДОБАВЛЕНО
}