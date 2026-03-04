package com.thatmg393.bettertpa4fabric.tpa.data;

import java.util.UUID;

import com.thatmg393.bettertpa4fabric.tpa.queue.RequestQueue;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class PlayerData {
    public final RequestQueue<UUID, BaseRequest> teleportRequests = new RequestQueue<>();
    
    public Pair<ServerWorld, BlockPos> previousTeleportPosition;
    public boolean isPlayerTeleporting = false;
}