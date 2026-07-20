package com.roserfwk.ccc.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import com.roserfwk.ccc.CustomCoyoteControl;
import com.roserfwk.ccc.data.rules.RuleManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadCommand.class)
public abstract class ReloadCommandMixin {
    @Inject(method = "lambda$register$0", at = @At("RETURN"))
    private static void ccc$onReload(
            CommandContext<CommandSourceStack> s,
            CallbackInfoReturnable<Integer> cir,
            @Local(name = "server") MinecraftServer server
    ) {
        CustomCoyoteControl.LOGGER.info("Reloading rules");
        RuleManager.getInstance(server).reload(server);
    }
}
