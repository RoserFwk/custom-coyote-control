package com.roserfwk.ccc.client.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.roserfwk.ccc.client.device.DeviceManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicInteger;

public final class DeviceCommands {
    private DeviceCommands() {}

    public static final LiteralArgumentBuilder<FabricClientCommandSource> DEVICE_CONNECT =
            ClientCommands.literal("connect").executes(ctx -> {
                if (DeviceManager.INSTANCE.isConnected()) {
                    ctx.getSource().sendError(Component.literal("Device already connected"));
                    return 0;
                }

                var ret = new AtomicInteger(Command.SINGLE_SUCCESS);

                DeviceManager.INSTANCE.connectAndInit().thenAccept(result -> {
                    if (result) {
                        ctx.getSource().sendFeedback(Component.literal("Successfully connected to the device"));
                    } else {
                        ctx.getSource().sendError(Component.literal("Failed to connect to the device"));
                        ret.set(0);
                    }
                }).join();

                return ret.get();
            });

    public static final LiteralArgumentBuilder<FabricClientCommandSource> DEVICE_DISCONNECT =
            ClientCommands.literal("disconnect").executes(ctx -> {
                if (!DeviceManager.INSTANCE.isConnected()) {
                    ctx.getSource().sendError(Component.literal("Not connected to the device"));
                    return 0;
                }

                DeviceManager.INSTANCE.detachDevice();
                ctx.getSource().sendFeedback(Component.literal("Successfully disconnected from the device"));

                return Command.SINGLE_SUCCESS;
            });

    public static final LiteralArgumentBuilder<FabricClientCommandSource> DEVICE_ROOT =
            ClientCommands.literal("device").then(DEVICE_CONNECT).then(DEVICE_DISCONNECT);

    public static final LiteralArgumentBuilder<FabricClientCommandSource> CCC_CLIENT_ROOT =
            ClientCommands.literal("ccc-client").then(DEVICE_ROOT);

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) ->
                dispatcher.register(CCC_CLIENT_ROOT)
        );
    }
}
