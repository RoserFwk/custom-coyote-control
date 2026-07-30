package com.roserfwk.ccc.mixin;

import com.roserfwk.ccc.data.criteria.impl.DamageCriterion;
import com.roserfwk.ccc.data.rules.RuleManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatTracker.class)
public abstract class CombatTrackerMixin {
    @Shadow
    @Final
    private LivingEntity mob;

    @Inject(method = "recordDamage", at = @At("TAIL"))
    private void ccc$onRecordDamage(DamageSource source, float damage, CallbackInfo ci) {
        if (mob instanceof ServerPlayer player) {
            RuleManager.getInstance(player.level().getServer())
                    .handleCriterion(DamageCriterion.class, criterion ->
                            criterion.recordDamageTaken(damage, source.typeHolder()));
        }
    }
}
