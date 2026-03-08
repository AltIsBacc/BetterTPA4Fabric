package com.thatmg393.bettertpa4fabric.tpa.data;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.thatmg393.bettertpa4fabric.tpa.queue.RequestQueue;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PlayerData {
    public final RequestQueue<UUID, BaseRequest> teleportRequests = new RequestQueue<>();
    
    @Nullable
    public Pair<RegistryKey<World>, BlockPos> previousTeleportPosition;

    public boolean isPlayerTeleporting = false;
    public boolean allowTeleportRequests = true;
}