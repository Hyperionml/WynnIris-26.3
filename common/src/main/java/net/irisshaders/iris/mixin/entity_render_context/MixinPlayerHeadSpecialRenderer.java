package net.irisshaders.iris.mixin.entity_render_context;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.pathways.WynncraftMountArmorOverlay;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHeadSpecialRenderer.class)
public class MixinPlayerHeadSpecialRenderer {
	@Shadow
	@Final
	private SkullModelBase modelBase;

	@Inject(method = "submit(Lnet/minecraft/client/renderer/PlayerSkinRenderCache$RenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
		at = @At("RETURN"))
	private void iris$submitWynncraftMountArmor(PlayerSkinRenderCache.RenderInfo renderInfo,
												PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
												int packedOverlay, boolean hasFoil, int color, CallbackInfo ci) {
		// 26.3: PlayerHeadSpecialRenderer#submit no longer receives an ItemDisplayContext;
		// this renderer only ever draws heads, so report HEAD.
		WynncraftMountArmorOverlay.submitPlayerHeadOverlays(renderInfo, ItemDisplayContext.HEAD, poseStack, submitNodeCollector, packedLight, modelBase);
	}
}
