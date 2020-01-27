package com.parzivail.smarttools;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.network.MessageType;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SmartTools implements ClientModInitializer
{
	private static boolean pauseUseHook = false;

	private static final String KEYBIND_CATEGORY = "key.smarttools.category";

	private static final Identifier KEY_TOGGLE_SWAP_ID = new Identifier("smarttools", "swap_tool");
	private static FabricKeyBinding keySwap;
	private static boolean swapEnabled;

	@Override
	public void onInitializeClient()
	{
		KeyBindingRegistryImpl.INSTANCE.addCategory(KEYBIND_CATEGORY);
		KeyBindingRegistryImpl.INSTANCE.register(keySwap = FabricKeyBinding.Builder.create(KEY_TOGGLE_SWAP_ID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, KEYBIND_CATEGORY).build());
	}

	public static void handleInputEvents()
	{
		while (keySwap.wasPressed())
		{
			swapEnabled = !swapEnabled;
			notifyChat(new TranslatableText(swapEnabled ? "smarttools.swap_tool_enabled" : "smarttools.swap_tool_disabled"));
		}
	}

	private static void notifyChat(TranslatableText text)
	{
		MinecraftClient.getInstance().inGameHud.addChatMessage(MessageType.GAME_INFO, text);
	}

	public static void onUse(CallbackInfo info, Runnable useItem)
	{
		if (pauseUseHook)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null || mc.player == null)
			return;

		ClientPlayerEntity p = mc.player;

		if (p.isHoldingSneakKey() && placeTorch(useItem, p))
			info.cancel();
	}

	public static void onAttack(CallbackInfo info)
	{
		if (!swapEnabled)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null || mc.player == null || mc.world == null)
			return;

		ClientPlayerEntity p = mc.player;

		if (!p.isHoldingSneakKey() && mc.options.keyAttack.isPressed())
			scrollToEffective(mc, p);
	}

	private static void scrollToEffective(MinecraftClient mc, ClientPlayerEntity p)
	{
		if (mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return;

		BlockState block = mc.world.getBlockState(((BlockHitResult)mc.crosshairTarget).getBlockPos());

		ItemStack stack = p.inventory.getMainHandStack();
		if (stack.getItem().isEffectiveOn(block))
			return;

		if (stack.getMiningSpeed(block) > 1)
			return;

		int effectiveSlot = -1;
		float maxSpeed = 1;
		for (int i = 0; i < 9; i++)
		{
			ItemStack queryStack = p.inventory.main.get(i);
			float speed = queryStack.getMiningSpeed(block);
			if (speed > maxSpeed)
			{
				maxSpeed = speed;
				effectiveSlot = i;
				break;
			}
		}

		if (effectiveSlot == -1)
			return;

		notifyChat(new TranslatableText("smarttools.switching_tool", p.inventory.main.get(effectiveSlot).getName()));

		int slotDiff = Math.abs(p.inventory.selectedSlot - effectiveSlot);
		boolean dir = p.inventory.selectedSlot > effectiveSlot;

		for (int i = 0; i < slotDiff; i++)
			p.inventory.scrollInHotbar(dir ? 1 : -1);
	}

	private static boolean placeTorch(Runnable useItem, ClientPlayerEntity p)
	{
		ItemStack stack = p.inventory.getMainHandStack();

		if (!(stack.getItem() instanceof MiningToolItem))
			return false;

		int torchSlot = -1;
		for (int i = 0; i < 9; i++)
		{
			ItemStack queryStack = p.inventory.main.get(i);
			if (queryStack.getItem() == Items.TORCH)
			{
				torchSlot = i;
				break;
			}
		}

		if (torchSlot == -1)
			return false;

		int slotDiff = Math.abs(p.inventory.selectedSlot - torchSlot);
		boolean dir = p.inventory.selectedSlot > torchSlot;

		for (int i = 0; i < slotDiff; i++)
			p.inventory.scrollInHotbar(dir ? 1 : -1);

		pauseUseHook = true;
		useItem.run();
		pauseUseHook = false;

		for (int i = 0; i < slotDiff; i++)
			p.inventory.scrollInHotbar(!dir ? 1 : -1);

		return true;
	}
}
