package me.noahvdaa.nochatlag.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiMixin {

	private final ExecutorService service = Executors.newFixedThreadPool(1);

	@Final
	@Shadow
	protected Minecraft minecraft;

	@Final
	@Shadow
	protected Map<ChatType, List<ChatListener>> chatListeners;

	@Shadow
	public abstract UUID guessChatUUID(Component message);

	@Inject(
			method = "handleChat(Lnet/minecraft/network/chat/ChatType;Lnet/minecraft/network/chat/Component;Ljava/util/UUID;)V",
			at = @At("HEAD"),
			cancellable = true
	)
	public void handleChat(ChatType chatType, Component chatComponent, UUID senderUUID, CallbackInfo ci) {
		service.submit(() -> {
			if (this.minecraft.isBlocked(senderUUID) || (this.minecraft.options.hideMatchedNames && this.minecraft.isBlocked(this.guessChatUUID(chatComponent)))) {
				return;
			}
			for (ChatListener chatListener : this.chatListeners.get(chatType)) {
				chatListener.handle(chatType, chatComponent, senderUUID);
			}
		});
		ci.cancel();
	}

}
