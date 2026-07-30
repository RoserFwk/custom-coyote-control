package com.roserfwk.ccc.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.roserfwk.ccc.data.criteria.impl.DamageCriterion;
import com.roserfwk.ccc.data.rules.RuleManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V"
            )
    )
    private void ccc$onAttack(
            Entity entity,
            CallbackInfo ci,
            @Local(name = "damageSource") DamageSource damageSource,
            @Local(name = "totalDamage") float totalDamage,
            @Local(name = "wasHurt") boolean wasHurt
    ) {
        var player = (Player) (Object) this;

        if (player.level().isClientSide()) {
            return;
        }

        if (!wasHurt) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            RuleManager.getInstance(serverPlayer.level().getServer())
                    .handleCriterion(DamageCriterion.class, criterion ->
                            criterion.recordDamage(totalDamage, damageSource.typeHolder()));
        }
    }
}
