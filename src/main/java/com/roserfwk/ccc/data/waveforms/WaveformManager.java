package com.roserfwk.ccc.data.waveforms;

import com.google.gson.Gson;
import com.roserfwk.ccc.CustomCoyoteControl;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WaveformManager implements ResourceManagerReloadListener {
    public static final WaveformManager INSTANCE = new WaveformManager();

    private static final Gson GSON = new Gson();

    private final Map<Identifier, Waveform> waveformMap = new HashMap<>();

    private WaveformManager() {}

    @Override
    public void onResourceManagerReload(@NonNull ResourceManager manager) {
        waveformMap.clear();

        for (var entry : manager.listResources(
                "waveforms",
                path -> path.toString().endsWith(".json")
        ).entrySet()) {
            try (var is = entry.getValue().open();
                 var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                GSON.fromJson(reader, Waveform.Raw.class).tryParse()
                        .ifPresent(waveform -> waveformMap.put(entry.getKey(), waveform));
            } catch (IOException e) {
                CustomCoyoteControl.LOGGER.warn(
                        "Failed to load waveform preset from {}", entry.getKey(), e
                );
            }
        }
    }

    public Optional<Waveform> getWaveform(Identifier id) {
        return Optional.ofNullable(waveformMap.get(id));
    }
}
