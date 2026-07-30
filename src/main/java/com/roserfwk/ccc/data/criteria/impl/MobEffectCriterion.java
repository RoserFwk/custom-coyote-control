package com.roserfwk.ccc.data.criteria.impl;

import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class MobEffectCriterion extends Criterion {
    private final String effectId;
    private final int minAmplifier;
    private final int minDuration;

    public MobEffectCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.MOB_EFFECT);
        this.effectId = (String) args.get("effect");
        this.minAmplifier = ((Number) args.getOrDefault("min_amplifier", 0)).intValue();
        this.minDuration = ((Number) args.getOrDefault("min_duration", 0)).intValue();
    }

    @Override
    public void update(CriterionContext context) {
        var effectIdentifier = Identifier.tryParse(effectId);
        if (effectIdentifier == null) {
            updateState(false);
            return;
        }

        var effect = BuiltInRegistries.MOB_EFFECT.get(effectIdentifier);
        if (effect.isEmpty()) {
            updateState(false);
            return;
        }

        var effectInstance = context.getTargetPlayer().getEffect(effect.get());
        var satisfied = effectInstance != null
                && effectInstance.getAmplifier() >= minAmplifier
                && effectInstance.getDuration() >= minDuration;

        updateState(satisfied);
    }
}
