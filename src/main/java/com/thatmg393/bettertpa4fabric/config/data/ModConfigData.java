package com.thatmg393.bettertpa4fabric.config.data;

public class ModConfigData {
    public int tpaCooldown = 5; // in seconds
    public int tpaExpireTime = 120; // in seconds;
    public int tpaTeleportTime = 5; // in seconds;

    public int tpaRequestLimit = 99;

    public boolean oneTimeTPABack = false;
    public boolean resetTimerOnMove = false;

    public int configVersion = 5; // internal value
}
