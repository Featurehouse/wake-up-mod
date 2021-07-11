package org.featurehouse.mcmod.wakeup.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.featurehouse.mcmod.wakeup.SleepManager;
import org.featurehouse.mcmod.wakeup.SleepSkipableWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
@Environment(EnvType.SERVER)
abstract class ServerWorldMixin extends World implements SleepSkipableWorld {
    @Shadow private boolean allPlayersSleeping;
    @Shadow @Final private List<ServerPlayerEntity> players;

    @Shadow public abstract void setTimeOfDay(long timeOfDay);

    @Shadow protected abstract void wakeSleepingPlayers();

    @Shadow protected abstract void resetWeather();

    private final SleepManager sleepManager = new SleepManager();

    @Inject(method = "updateSleepingPlayers",
    at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"),
    cancellable = true)
    @SuppressWarnings("mixin-compatibility")
    private void handlePlayerSleeping(CallbackInfo ci) {
        if (this.sleepManager.update(this.players) &&
                this.wakeUp_isSleepingEnabled()) {
            int i = this.getGameRules().getInt(SleepManager.RULE);
            TranslatableText text = this.sleepManager.canSkipNight(i)
                    ? new TranslatableText("wake_up.sleep.skipping_night")
                    : new TranslatableText(
                            "wake_up.sleep.players_sleeping",
                    sleepManager.getSleeping(), sleepManager.getNightSkippingRequirement(i)
            );
            for (ServerPlayerEntity player : this.players) {
                player.sendMessage(text, true);
            }
        } ci.cancel();
    }

    @Inject(method = "tick",
    at = @At(value = "FIELD", target = "net.minecraft.server.world.ServerWorld.allPlayersSleeping:Z"))
    private void tickSleepManager(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        // 663: aload_0 // this
        // 664: getfield allplayersSleeping:Z
        // 667: ifeq 759
        this.allPlayersSleeping = false;    // trigger bytecode 667
        int gamerule = this.getGameRules().getInt(SleepManager.RULE);
        if (this.sleepManager.canSkipNight(gamerule)
                && this.sleepManager.canResetTime(gamerule, this.players)) {
            if (this.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
                long skip = this.properties.getTimeOfDay() + 24000L;
                this.setTimeOfDay(skip - skip % 24000L);
            }
            this.wakeSleepingPlayers();
            if (this.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE))
                this.resetWeather();
        } // DONE
    }

    @Inject(at = @At("HEAD"), method = "wakeSleepingPlayers()V")
    private void clearBeforeWake(CallbackInfo ci) {
        this.sleepManager.clearSleeping();
    }

    // --*-- --*-- --*-- //

    @Override
    public SleepManager wakeUp_getSleepManager() {
        return sleepManager;
    }

    @Override
    public boolean wakeUp_isSleepingEnabled() {
        return this.getGameRules().getInt(SleepManager.RULE) <= 100;
    }

    @Deprecated
    private ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }
}
