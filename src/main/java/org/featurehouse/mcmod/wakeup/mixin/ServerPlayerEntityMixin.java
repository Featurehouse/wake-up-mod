package org.featurehouse.mcmod.wakeup.mixin;

import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import org.featurehouse.mcmod.wakeup.SleepSkipableWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
@Environment(EnvType.SERVER)
abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public abstract void sendMessage(Text message, boolean actionBar);

    @Inject(method = "trySleep",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;updateSleepingPlayers()V"))
    private void sleepTrialEnabling(BlockPos pos, CallbackInfoReturnable<Either<SleepFailureReason, Unit>> cir) {
        if (!((SleepSkipableWorld) this.getServerWorld()).wakeUp_isSleepingEnabled())
            this.sendMessage(new TranslatableText("wake_up.sleep.not_possible"), true);
    }

    // --*-- --*-- --*-- //

    @Deprecated @SuppressWarnings("all")
    private ServerPlayerEntityMixin() {
        super(null, null, 0.0F, null);
    }
}
