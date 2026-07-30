package com.roserfwk.ccc.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.roserfwk.ccc.data.criteria.impl.DamageCriterion;
import com.roserfwk.ccc.data.rules.RuleManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile {
    public AbstractArrowMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract void setOwner(@Nullable Entity owner);

    @Inject(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;doKnockback(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)V"
            )
    )
    private void ccc$onHitEntity(
            EntityHitResult hitResult,
            CallbackInfo ci,
            @Local(name = "damageSource") DamageSource damageSource,
            @Local(name = "damage") int damage
    ) {
        if (this.level().isClientSide()) {
            return;
        }

        var owner = this.getOwner();
        if (owner instanceof ServerPlayer player) {
            RuleManager.getInstance(player.level().getServer())
                    .handleCriterion(DamageCriterion.class, criterion ->
                            criterion.recordDamage(damage, damageSource.typeHolder()));
        }
    }
}
