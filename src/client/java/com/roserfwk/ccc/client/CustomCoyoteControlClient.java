package com.roserfwk.ccc.client;

import com.roserfwk.ccc.client.commands.DeviceCommands;
import com.roserfwk.ccc.client.device.DeviceManager;
import com.roserfwk.ccc.network.ClientboundTriggerCoyotePacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomCoyoteControlClient implements ClientModInitializer {
	public static final Logger LOGGER
			= LoggerFactory.getLogger("custom-coyote-control-client");

	@Override
	public void onInitializeClient() {
		DeviceCommands.register();

		ClientLifecycleEvents.CLIENT_STOPPING.register((_) ->
				DeviceManager.INSTANCE.detach());

		ClientPlayNetworking.registerGlobalReceiver(ClientboundTriggerCoyotePacket.TYPE, ((payload, _) -> {
			var deviceManager = DeviceManager.INSTANCE;

			if (deviceManager.isConnected()) {
				deviceManager.handlePayload(payload);
			}
		}));
	}
}