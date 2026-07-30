package com.roserfwk.ccc.data.criteria.impl;

import com.google.gson.JsonObject;
import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DamageCriterion extends Criterion {
    private final float minDamage;
    private final @Nullable Identifier damageTypeId;
    private final boolean dealDamage;
    private float accumulatedDamage = 0f;
    private long lastResetTick = 0;

    public DamageCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.DAMAGE);
        this.minDamage = ((Number) args.get("min_damage")).floatValue();
        this.dealDamage = (Boolean) args.getOrDefault("deal_damage", false);

        var damageTypeString = (String) args.get("damage_type");
        if (damageTypeString == null) {
            this.damageTypeId = null;
        } else {
            this.damageTypeId = Identifier.tryParse(damageTypeString);
        }
    }

    @Override
    public void update(CriterionContext context) {
        var currentTick = context.getGameTime();

        if (currentTick != lastResetTick) {
            updateState(accumulatedDamage >= minDamage);
            accumulatedDamage = 0f;
            lastResetTick = currentTick;
        }
    }

    public void recordDamage(float damage, Holder<DamageType> damageType) {
        if (dealDamage) {
            if (damageTypeId == null || damageType.is(damageTypeId)) {
                this.accumulatedDamage += damage;
            }
        }
    }

    public void recordDamageTaken(float damage, Holder<DamageType> damageType) {
        if (!dealDamage) {
            if (damageTypeId == null || damageType.is(damageTypeId)) {
                this.accumulatedDamage += damage;
            }
        }
    }

    @Override
    public JsonObject serializeState() {
        var obj = super.serializeState();
        obj.addProperty("accumulated_damage", accumulatedDamage);
        obj.addProperty("last_reset_tick", lastResetTick);
        return obj;
    }

    @Override
    public void deserializeState(JsonObject json) {
        super.deserializeState(json);
        accumulatedDamage = json.get("accumulated_damage").getAsFloat();
        lastResetTick = json.get("last_reset_tick").getAsLong();
    }
}
