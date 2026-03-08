package com.thatmg393.bettertpa4fabric.config.data;

import com.thatmg393.bettertpa4fabric.config.annotations.ConfigComment;

public class ModConfigData {
    @ConfigComment("How long before a pending TPA request expires, in seconds")
    public int tpaExpireTime = 120;

    @ConfigComment("How long the teleport countdown lasts, in seconds")
    public int tpaTeleportTime = 5;

    @ConfigComment("If true, /tpaback can only be used once per teleport")
    public boolean oneTimeTPABack = false;

    @ConfigComment("If true, moving during countdown resets the timer instead of cancelling")
    public boolean resetTimerOnMove = false;

    @ConfigComment("How long before a player can send another TPA request, in seconds (deprecated)")
    public int tpaCooldown = 5;

    @ConfigComment("Maximum number of simultaneous incoming requests a player can have (deprecated)")
    public int tpaRequestLimit = 99;

    public int configVersion = 5;
}
