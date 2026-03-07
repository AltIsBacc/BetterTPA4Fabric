package com.thatmg393.bettertpa4fabric;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thatmg393.bettertpa4fabric.command.argument.TPAArgumentType;
import com.thatmg393.bettertpa4fabric.config.ModConfigManager;
import com.thatmg393.bettertpa4fabric.config.data.ModConfigData;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;

public class BetterTPA4Fabric implements DedicatedServerModInitializer {
    public static final String MOD_ID = "bettertpa4fabric";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ModConfigData CONFIG = ModConfigManager.loadOrGetConfig();

    @Override
    public void onInitializeServer() {
        LOGGER.info("xin, here i am!");
        TeleportManager.INSTANCE.init();
        LOGGER.info("if 1 + 2 is 3 then 2 + 1 is 2");
        LOGGER.info("Using BetterTPA4Fabric " + FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion().getFriendlyString());

        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(
                literal("tpa")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .then(
                    argument("to", StringArgumentType.word())
                    .suggests((ctx, builder) -> TPAArgumentType.ALLOWED_PLAYERS.listSuggestions(ctx, builder))
                    .executes(ctx -> TeleportManager.INSTANCE.teleportTo(
                        ctx.getSource().getPlayer(),
                        TPAArgumentType.ALLOWED_PLAYERS.resolve(ctx, "to")
                    ))
                )
            );

            dispatcher.register(
                literal("tpahere")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .then(
                    argument("who", StringArgumentType.word())
                    .suggests((ctx, builder) -> TPAArgumentType.ALLOWED_PLAYERS.listSuggestions(ctx, builder))
                    .executes(ctx -> TeleportManager.INSTANCE.teleportHere(
                        ctx.getSource().getPlayer(),
                        TPAArgumentType.ALLOWED_PLAYERS.resolve(ctx, "who")
                    ))
                )
            );

            dispatcher.register(
                literal("tpaback")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(ctx -> TeleportManager.INSTANCE.teleportBack(ctx.getSource().getPlayer()))
            );

            dispatcher.register(
                literal("tpaaccept")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .then(
                    argument("from", StringArgumentType.word())
                    .suggests((ctx, builder) -> TPAArgumentType.INCOMING_REQUESTS.listSuggestions(ctx, builder))
                    .executes(ctx -> TeleportManager.INSTANCE.acceptTeleport(
                        ctx.getSource().getPlayer(),
                        TPAArgumentType.INCOMING_REQUESTS.resolve(ctx, "from")
                    ))
                )
                .executes(ctx -> TeleportManager.INSTANCE.acceptTeleport(ctx.getSource().getPlayer(), null))
            );

            dispatcher.register(
                literal("tpadeny")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .then(
                    argument("from", StringArgumentType.word())
                    .suggests((ctx, builder) -> TPAArgumentType.INCOMING_REQUESTS.listSuggestions(ctx, builder))
                    .executes(ctx -> TeleportManager.INSTANCE.denyTeleport(
                        ctx.getSource().getPlayer(),
                        TPAArgumentType.INCOMING_REQUESTS.resolve(ctx, "from")
                    ))
                )
                .executes(ctx -> TeleportManager.INSTANCE.denyTeleport(ctx.getSource().getPlayer(), null))
            );

			dispatcher.register(
				literal("tpaallow")
				.requires(ServerCommandSource::isExecutedByPlayer)
				.then(
					argument("isAllowed", BoolArgumentType.bool())
					.executes(ctx -> TeleportManager.INSTANCE.allowTeleport(ctx.getSource().getPlayer(), BoolArgumentType.getBool(ctx, "isAllowed")))
				)
				.executes(ctx -> TeleportManager.INSTANCE.allowTeleport(ctx.getSource().getPlayer(), null))
			);

			/*
			dispatcher.register(
				literal("tpaconfig")
				.requires(src -> src.hasPermissionLevel(4))
				.then(
					argument("key", StringArgumentType.string())
					.then(
						argument("value", IntegerArgumentType.integer()) // TODO: Replace IntegerArgumentType to AnyArgumentType
						.executes(ctx -> 1)
					)
				)
			);
			*/
		});
	}
}
