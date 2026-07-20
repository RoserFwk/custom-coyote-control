package com.roserfwk.ccc.data.rules;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.roserfwk.ccc.CustomCoyoteControl;
import com.roserfwk.ccc.data.waveforms.Waveform;
import com.roserfwk.ccc.data.waveforms.WaveformManager;
import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import com.roserfwk.ccc.network.ClientboundTriggerCoyotePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class Rule implements Cloneable {
    private String name;
    private String description;
    private @JsonAdapter(CriteriaDeserializer.class) Map<String, Criterion> criteria;
    private Effect effect;
    private long cooldown;

    public boolean update(CriterionContext context) {
        for (var criterion : criteria.values()) {
            criterion.update(context);
        }

        var allTriggered = criteria.values().stream().allMatch(Criterion::shouldTrigger);

        if (allTriggered) {
            var player = context.getTargetPlayer();
            execute(player);

            resetCriteria();

            return true;
        }

        return false;
    }

    public void forceTrigger(ServerPlayer player) {
        execute(player);
        resetCriteria();
    }

    public void resetCriteria() {
        for (var criterion : criteria.values()) {
            criterion.reset();
        }
    }

    private void execute(ServerPlayer player) {
        var aChannel = effect.a();
        if (aChannel != null) {
            ServerPlayNetworking.send(player, new ClientboundTriggerCoyotePacket(
                    Channel.A,
                    aChannel.interval(),
                    aChannel.waveform(),
                    aChannel.coefficient()
            ));
        }

        var bChannel = effect.b();
        if (bChannel != null) {
            ServerPlayNetworking.send(player, new ClientboundTriggerCoyotePacket(
                    Channel.B,
                    bChannel.interval(),
                    bChannel.waveform(),
                    bChannel.coefficient()
            ));
        }
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public long cooldown() {
        return cooldown;
    }

    public Map<String, Criterion> criteriaWithIds() {
        return criteria;
    }

    public Collection<Criterion> criteria() {
        return criteria.values();
    }

    public record Effect(
            ChannelSettings a,
            ChannelSettings b
    ) {}

    public record ChannelSettings(
            long interval,
            @JsonAdapter(WaveformDeserializer.class) Waveform waveform,
            double coefficient
    ) {}

    static class CriteriaDeserializer implements JsonDeserializer<Map<String, Criterion>> {
        @Override
        public Map<String, Criterion> deserialize(
                JsonElement json,
                Type typeOfT,
                JsonDeserializationContext context
        ) throws JsonParseException {
            if (json.isJsonObject()) {
                var raw = new HashMap<String, Criterion.Raw>();

                for (var entry : json.getAsJsonObject().entrySet()) {
                    var id = entry.getKey();
                    var elem = entry.getValue();

                    if (!elem.isJsonObject()) {
                        CustomCoyoteControl.LOGGER.warn("Invalid json array element: {}", elem);
                        return Map.of();
                    }
                    var object = elem.getAsJsonObject();

                    var type = object.get("type");
                    if (type == null) {
                        CustomCoyoteControl.LOGGER.warn("Missing type: {}", elem);
                        return Map.of();
                    }

                    var args = object.get("args");
                    if (args == null || !args.isJsonObject()) {
                        CustomCoyoteControl.LOGGER.warn("Invalid criterion args: {}", elem);
                        return Map.of();
                    }

                    var mapType = new TypeToken<Map<String, Object>>() {}.getType();
                    raw.put(id, new Criterion.Raw(
                            type.getAsString(),
                            context.deserialize(args, mapType)
                    ));
                }

                var stream = raw.entrySet().stream().map(entry ->
                        Map.entry(entry.getKey(), entry.getValue().tryBuild()));
                if (stream.anyMatch(entry -> entry.getValue().isEmpty())) {
                    CustomCoyoteControl.LOGGER.warn("Invalid criteria: {}", json);
                    //noinspection DataFlowIssue
                    stream.forEach(_ -> {});
                    return Map.of();
                }

                //noinspection DataFlowIssue
                return stream.map(entry ->
                        Map.entry(entry.getKey(), entry.getValue().get())
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }

            CustomCoyoteControl.LOGGER.warn("Failed to parse criteria: {}", json);
            return Map.of();
        }
    }

    static class WaveformDeserializer implements JsonDeserializer<Waveform> {
        @Override
        public Waveform deserialize(
                JsonElement json,
                Type typeOfT,
                JsonDeserializationContext context
        ) throws JsonParseException {
            if (json.isJsonArray()) {
                var raw = new ArrayList<String>();

                for (var elem : json.getAsJsonArray()) {
                    raw.add(elem.getAsString());
                }

                var waveform = new Waveform.Raw(raw).tryParse();
                if (waveform.isEmpty()) {
                    CustomCoyoteControl.LOGGER.warn("Invalid raw waveform: '{}'", json);
                    return null;
                }
                return waveform.get();
            } else if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                var waveform = WaveformManager.INSTANCE.getWaveform(
                        Identifier.tryParse(json.getAsString())
                );

                if (waveform.isEmpty()) {
                    CustomCoyoteControl.LOGGER.warn("Failed to find waveform preset: {}", json);
                    return null;
                }

                return waveform.get();
            }

            CustomCoyoteControl.LOGGER.warn("Invalid waveform JSON: '{}'", json);
            return null;
        }
    }

    public Rule clone() {
        try {
            return (Rule) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Channel {
        A,
        B;

        public static final StreamCodec<FriendlyByteBuf, Channel> CODEC = StreamCodec.of(
                FriendlyByteBuf::writeEnum,
                (buf) -> buf.readEnum(Channel.class)
        );
    }
}
