package com.roserfwk.ccc.data.waveforms;

import com.mojang.datafixers.util.Pair;
import com.roserfwk.ccc.CustomCoyoteControl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Waveform(List<Pair<int[], int[]>> waveform) {
    public static final StreamCodec<FriendlyByteBuf, Waveform> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.waveform().size());

                for (var pair : value.waveform()) {
                    buf.writeVarIntArray(pair.getFirst());
                    buf.writeVarIntArray(pair.getSecond());
                }
            },
            (buf) -> {
                var waveform = new ArrayList<Pair<int[], int[]>>();

                var size = buf.readVarInt();
                for (int i = 0; i < size; i++) {
                    var freq = buf.readVarIntArray();
                    var str = buf.readVarIntArray();

                    waveform.add(new Pair<>(freq, str));
                }

                return new Waveform(waveform);
            }
    );

    public record Raw(List<String> waveform) {
        public Optional<Waveform> tryParse() {
            var parsedWaveform = waveform.stream().map(s -> {
                if (s.length() != 16) {
                    CustomCoyoteControl.LOGGER.warn(
                            "Failed to parse \"{}\", waveform length must be 16 characters long", s
                    );
                    return null;
                }

                var freq = new int[4];
                var str = new int[4];

                try {
                    for (int i = 0; i < 8; i++) {
                        var hex = s.substring(i * 2, (i * 2) + 2);
                        var value = Integer.parseInt(hex, 16);

                        if (i < 4) {
                            freq[i] = value;
                        } else {
                            str[i - 4] = value;
                        }
                    }
                } catch (NumberFormatException e) {
                    CustomCoyoteControl.LOGGER.warn("Failed to parse waveform from string", e);
                    return null;
                }

                return new Pair<>(freq, str);
            }).toList();

            if (parsedWaveform.contains(null)) {
                CustomCoyoteControl.LOGGER.warn("Waveform contains invalid points");
                return Optional.empty();
            }

            return Optional.of(new Waveform(parsedWaveform));
        }
    }
}
