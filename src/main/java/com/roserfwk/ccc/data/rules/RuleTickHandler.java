package com.roserfwk.ccc.data.rules;

import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public final class RuleTickHandler {
    private RuleTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RuleTickHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) {
            var context = new CriterionContext(server, player);
            RuleManager.getInstance(server).updateRules(context);
        }
    }
}
