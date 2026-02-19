package com.thatmg393.bettertpa4fabric.tpa;

import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class PlayerData {
    private static final Object2ObjectOpenHashMap<UUID, PlayerData> dataMap = new Object2ObjectOpenHashMap<>();

    public static PlayerData get(UUID uuid) {
        return dataMap.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public static void remove(UUID uuid) {
        dataMap.remove(uuid);
    }

    public ServerWorld lastWorld;
    public BlockPos lastPos;
    
    public ServerWorld homeWorld;
    public BlockPos homePos;

    public boolean hasLastLocation() {
        return lastWorld != null && lastPos != null;
    }

    public boolean hasHome() {
        return homeWorld != null && homePos != null;
    }

    public void saveLastLocation(ServerWorld world, BlockPos pos) {
        this.lastWorld = world;
        this.lastPos = pos;
    }

    public void setHome(ServerWorld world, BlockPos pos) {
        this.homeWorld = world;
        this.homePos = pos;
    }
}
