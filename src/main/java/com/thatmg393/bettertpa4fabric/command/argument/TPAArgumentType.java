package com.thatmg393.bettertpa4fabric.command.argument;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.thatmg393.bettertpa4fabric.tpa.TeleportManager;
import com.thatmg393.bettertpa4fabric.tpa.data.PlayerData;
import com.thatmg393.bettertpa4fabric.utils.MCTextUtils;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class TPAArgumentType implements ArgumentType<String> {

    public enum Mode {
        /** Suggests players who have sent YOU a request (for tpaaccept/tpadeny) */
        INCOMING_REQUESTS,
        /** Suggests all allowed players except yourself (for tpa/tpahere) */
        ALLOWED_PLAYERS
    }

    private final Mode mode;

    private TPAArgumentType(Mode mode) {
        this.mode = mode;
    }

    public static TPAArgumentType incomingRequests() {
        return new TPAArgumentType(Mode.INCOMING_REQUESTS);
    }

    public static TPAArgumentType allowedPlayers() {
        return new TPAArgumentType(Mode.ALLOWED_PLAYERS);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context,
        SuggestionsBuilder builder
    ) {
        if (!(context.getSource() instanceof ServerCommandSource source)) {
            return Suggestions.empty();
        }

        ServerPlayerEntity self = source.getPlayer();
        if (self == null) return Suggestions.empty();

        switch (mode) {
            case INCOMING_REQUESTS -> {
                PlayerData data = TeleportManager.INSTANCE.getPlayerData(self.getUuid());
                data.teleportRequests.entrySet().stream()
                    .filter(e -> !e.getValue().isExpired())
                    .map(e -> e.getValue().getRequester())
                    .filter(p -> p.networkHandler.isConnectionOpen())
                    .map(ServerPlayerEntity::getNameForScoreboard)
                    .filter(name -> name.startsWith(builder.getRemaining()))
                    .forEach(builder::suggest);
            }

            case ALLOWED_PLAYERS -> {
                TeleportManager.INSTANCE.streamPlayerDatas()
                    .filter(e -> !e.getKey().equals(self.getUuid()))
                    .filter(e -> e.getValue().allowTeleportRequests)
                    .map(e -> source.getServer().getPlayerManager().getPlayer(e.getKey()))
                    .filter(p -> p != null)
                    .map(ServerPlayerEntity::getNameForScoreboard)
                    .filter(name -> name.startsWith(builder.getRemaining())) 
                    .forEach(builder::suggest);
            }
        }

        return builder.buildFuture();
    }

    /**
     * Call this in your command method to resolve the string arg back to a player.
     * Throws a user-visible error if the player is offline.
     */
    public static ServerPlayerEntity resolve(
        CommandContext<ServerCommandSource> ctx,
        String argName,
        Mode mode
    ) throws CommandSyntaxException {
        String name = ctx.getArgument(argName, String.class);
        ServerPlayerEntity player = ctx.getSource().getServer().getPlayerManager().getPlayer(name);

        if (player == null) {
            throw new SimpleCommandExceptionType(
                MCTextUtils.fromLang("bettertpa4fabric.message.error.target_offline", name)
            ).create();
        }

        ServerPlayerEntity self = ctx.getSource().getPlayer();
        switch (mode) {
            case INCOMING_REQUESTS -> {
                PlayerData selfData = TeleportManager.INSTANCE.getPlayerData(self.getUuid());
                
                if (!selfData.teleportRequests.containsKey(player.getUuid())) {
                    throw new SimpleCommandExceptionType(
                        MCTextUtils.fromLang("bettertpa4fabric.message.error.no_request_from_player", name)
                    ).create();
                }
            }

            case ALLOWED_PLAYERS -> {
                if (player.equals(self)) {
                    throw new SimpleCommandExceptionType(
                        MCTextUtils.fromLang("bettertpa4fabric.message.error.tpa_to_self")
                    ).create();
                }
                PlayerData targetData = TeleportManager.INSTANCE.getPlayerData(player.getUuid());
                if (!targetData.allowTeleportRequests) {
                    throw new SimpleCommandExceptionType(
                        MCTextUtils.fromLang("bettertpa4fabric.message.error.tpa_not_allowed")
                    ).create();
                }
            }
        }

        return player;
    }
}
