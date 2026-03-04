package com.thatmg393.bettertpa4fabric.tpa;

import com.thatmg393.bettertpa4fabric.config.data.ModConfigData;
import com.thatmg393.bettertpa4fabric.tpa.data.PlayerData;
import com.thatmg393.bettertpa4fabric.tpa.request.base.BaseRequest;
import com.thatmg393.bettertpa4fabric.tpa.tickable.task.TeleportTask;
import com.thatmg393.bettertpa4fabric.utils.Either;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TeleportManagerTest {

    // -------------------------------------------------------------------------
    // Stub request — avoids touching any Minecraft class
    // -------------------------------------------------------------------------
    static class StubRequest extends BaseRequest {
        StubRequest() { super(null, Either.left(null)); }

        @Override public TeleportTask accept() { return null; }
    }

    // -------------------------------------------------------------------------
    // Helper: create a BaseRequest whose createdAt is artificially in the past
    // -------------------------------------------------------------------------
    static StubRequest requestWithAge(long ageMillis) throws Exception {
        StubRequest req = new StubRequest();
        Field f = BaseRequest.class.getDeclaredField("createdAt");
        f.setAccessible(true);
        f.set(req, System.currentTimeMillis() - ageMillis);
        return req;
    }

    // -------------------------------------------------------------------------
    // Helper: inject a ModConfigData into BetterTPA4Fabric.CONFIG
    // -------------------------------------------------------------------------
    static void setConfig(ModConfigData cfg) throws Exception {
        Field f = com.thatmg393.bettertpa4fabric.BetterTPA4Fabric.class.getDeclaredField("CONFIG");
        // CONFIG is a `public static final` field — we need to remove the final modifier
        f.setAccessible(true);

        // Use Unsafe or simply set via reflection (works on JDK 17+ with --add-opens)
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        } catch (NoSuchFieldException ignored) {
            // JDK 12+: modifiers field is hidden; the set() call below will still work
            // because the field was already made accessible
        }
        f.set(null, cfg);
    }

    private TeleportManager manager;

    @BeforeEach
    void setUp() {
        // Each test gets a fresh TeleportManager so state doesn't bleed between tests.
        manager = new TeleportManager();
    }

    // -------------------------------------------------------------------------
    // getPlayerData
    // -------------------------------------------------------------------------

    @Test
    void getPlayerData_newUUID_returnsEmptyPlayerData() {
        UUID id = UUID.randomUUID();
        PlayerData data = manager.getPlayerData(id);

        assertNotNull(data);
        assertTrue(data.teleportRequests.isEmpty());
        assertNull(data.previousTeleportPosition);
        assertFalse(data.isPlayerTeleporting);
    }

    @Test
    void getPlayerData_sameUUID_returnsSameInstance() {
        UUID id = UUID.randomUUID();
        PlayerData first  = manager.getPlayerData(id);
        PlayerData second = manager.getPlayerData(id);

        assertSame(first, second, "must return the same PlayerData for the same UUID");
    }

    @Test
    void getPlayerData_differentUUIDs_returnsDifferentInstances() {
        PlayerData a = manager.getPlayerData(UUID.randomUUID());
        PlayerData b = manager.getPlayerData(UUID.randomUUID());

        assertNotSame(a, b);
    }

    @Test
    void getPlayerData_mutationsArePersisted() {
        UUID id = UUID.randomUUID();
        manager.getPlayerData(id).isPlayerTeleporting = true;

        assertTrue(manager.getPlayerData(id).isPlayerTeleporting);
    }

    // -------------------------------------------------------------------------
    // BaseRequest.isExpired
    // -------------------------------------------------------------------------

    @Test
    void isExpired_freshRequest_isFalse() throws Exception {
        ModConfigData cfg = new ModConfigData();
        cfg.tpaExpireTime = 120;
        setConfig(cfg);

        StubRequest req = new StubRequest();
        assertFalse(req.isExpired(), "a brand-new request must not be expired");
    }

    @Test
    void isExpired_requestOlderThanExpireTime_isTrue() throws Exception {
        ModConfigData cfg = new ModConfigData();
        cfg.tpaExpireTime = 2; // 2 seconds
        setConfig(cfg);

        // Simulate a request created 3 seconds ago
        StubRequest req = requestWithAge(3_000);
        assertTrue(req.isExpired());
    }

    @Test
    void isExpired_requestExactlyAtBoundary_isFalse() throws Exception {
        ModConfigData cfg = new ModConfigData();
        cfg.tpaExpireTime = 5;
        setConfig(cfg);

        // 1 ms before expiry — still valid
        StubRequest req = requestWithAge((cfg.tpaExpireTime * 1000L) - 1);
        assertFalse(req.isExpired());
    }

    @Test
    void isExpired_zeroExpireTime_expiredImmediately() throws Exception {
        ModConfigData cfg = new ModConfigData();
        cfg.tpaExpireTime = 0;
        setConfig(cfg);

        // Any age ≥ 0 ms → expired
        StubRequest req = requestWithAge(1);
        assertTrue(req.isExpired());
    }
}
