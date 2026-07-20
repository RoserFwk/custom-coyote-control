package com.roserfwk.ccc.network;

import com.roserfwk.ccc.CustomCoyoteControl;
import com.roserfwk.ccc.data.rules.Rule;
import com.roserfwk.ccc.data.waveforms.Waveform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record ClientboundTriggerCoyotePacket(
        Rule.Channel channel,
        long interval,
        Waveform waveform,
        double coefficient
) implements CustomPacketPayload {
    public static final Type<ClientboundTriggerCoyotePacket> TYPE =
            new Type<>(CustomCoyoteControl.id("trigger_coyote"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundTriggerCoyotePacket> CODEC = StreamCodec.composite(
            Rule.Channel.CODEC, ClientboundTriggerCoyotePacket::channel,
            ByteBufCodecs.LONG, ClientboundTriggerCoyotePacket::interval,
            Waveform.CODEC, ClientboundTriggerCoyotePacket::waveform,
            ByteBufCodecs.DOUBLE, ClientboundTriggerCoyotePacket::coefficient,
            ClientboundTriggerCoyotePacket::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
