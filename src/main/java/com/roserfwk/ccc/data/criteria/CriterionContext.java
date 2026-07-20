package com.roserfwk.ccc.data.criteria;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class CriterionContext {
    private final MinecraftServer server;
    private final ServerPlayer targetPlayer;
    private final List<ServerPlayer> onlinePlayers;
    private final long gameTime;
    private final ServerLevel level;

    public CriterionContext(MinecraftServer server, ServerPlayer targetPlayer) {
        this.server = server;
        this.targetPlayer = targetPlayer;
        this.onlinePlayers = server.getPlayerList().getPlayers();
        this.gameTime = server.overworld().getGameTime();
        this.level = targetPlayer.level();
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerPlayer getTargetPlayer() {
        return targetPlayer;
    }

    public List<ServerPlayer> getOnlinePlayers() {
        return onlinePlayers;
    }

    public long getGameTime() {
        return gameTime;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public UUID getTargetPlayerUuid() {
        return targetPlayer.getUUID();
    }
}
