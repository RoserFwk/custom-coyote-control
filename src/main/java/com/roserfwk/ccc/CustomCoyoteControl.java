package com.roserfwk.ccc;

import com.roserfwk.ccc.commands.ControlCommands;
import com.roserfwk.ccc.data.criteria.impl.Criteria;
import com.roserfwk.ccc.data.rules.RuleTickHandler;
import com.roserfwk.ccc.data.waveforms.WaveformManager;
import com.roserfwk.ccc.network.ClientboundTriggerCoyotePacket;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;

import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomCoyoteControl implements ModInitializer {
	public static final String MOD_ID = "custom-coyote-control";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(
				ClientboundTriggerCoyotePacket.TYPE,
				ClientboundTriggerCoyotePacket.CODEC
		);

		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
				id("waveforms"),
                WaveformManager.INSTANCE
		);

		RuleTickHandler.register();

		ControlCommands.register();

		Criteria.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
