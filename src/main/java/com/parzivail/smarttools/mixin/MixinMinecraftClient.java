package com.parzivail.smarttools.mixin;

import com.parzivail.smarttools.SmartTools;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient
{
	@Shadow
	public abstract void doItemUse();

	@Inject(at = @At("HEAD"), method = "handleBlockBreaking")
	private void onAttack(CallbackInfo info)
	{
		SmartTools.onAttack(info);
	}

	@Inject(at = @At("HEAD"), method = "doItemUse", cancellable = true)
	private void onUse(CallbackInfo info)
	{
		SmartTools.onUse(info, this::doItemUse);
	}

	@Inject(method = "handleInputEvents", at = @At("HEAD"))
	private void handleInputEvents(CallbackInfo info)
	{
		SmartTools.handleInputEvents();
	}
}
