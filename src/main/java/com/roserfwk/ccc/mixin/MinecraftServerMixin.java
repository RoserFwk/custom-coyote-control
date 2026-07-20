package com.roserfwk.ccc.mixin;

import com.mojang.datafixers.DataFixer;
import com.roserfwk.ccc.data.rules.RuleManager;
import com.roserfwk.ccc.utils.CCCMinecraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements CCCMinecraftServer {
    @Unique
    private /*final*/ RuleManager ruleManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ccc$onServerInit(
            Thread serverThread,
            LevelStorageSource.LevelStorageAccess storageSource,
            PackRepository packRepository,
            WorldStem worldStem,
            Optional<GameRules> gameRules,
            Proxy proxy,
            DataFixer fixerUpper,
            Services services,
            LevelLoadListener levelLoadListener,
            boolean propagatesCrashes,
            NotificationManager notificationManager,
            CallbackInfo ci
    ) {
        ruleManager = new RuleManager();
        var server = (MinecraftServer) (Object) this;
        ruleManager.reload(server);
        ruleManager.initPersistentData(server);
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void ccc$onStopServer(CallbackInfo ci) {
        ruleManager.savePersistentData((MinecraftServer) (Object) this);
    }

    @Override
    public RuleManager ccc$getRuleManager() {
        return ruleManager;
    }
}
