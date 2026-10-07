package net.irisshaders.iris.mixin.entity_render_context;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntList;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects Wynncraft translucent VFX item layers and arms the "forced item layer" signal so the
 * translucency deferral path can route those draws specially.
 * <p>
 * 26.3: This mixin used to target {@code ItemRenderer.renderItem} and wrap the item's RenderType
 * via {@code MultiBufferSource.getBuffer(...)} (using {@code OuterWrappedRenderType} +
 * {@code WynncraftVfxRenderStateShard}). Both {@code net.minecraft.client.renderer.entity.ItemRenderer}
 * and the whole {@code MultiBufferSource} immediate-mode BufferSource path were removed in 26.3: item
 * layers are now resolved into {@code ItemStackRenderState} and submitted through
 * {@code SubmitNodeCollector}. The mixin is therefore retargeted to
 * {@code ItemStackRenderState.LayerRenderState.submit} and keeps the begin/end signal bookkeeping
 * ({@link ImmediateState#beginForcedWynncraftItemLayerSignal}) which is independent of the buffer
 * path. The RenderType-wrapping step is DROPPED because there is no equivalent per-item
 * MultiBufferSource hook to attach it to; the Wynncraft VFX deferral consumers that read this
 * signal (MixinBufferSource) were built on the removed immediate-mode path as well.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public class MixinItemRenderer_VfxTranslucency {
	@Shadow
	private ItemQuads quads;

	@Shadow
	private IntList tintLayers;

	@Inject(method = "submit", at = @At("HEAD"))
	private void iris$beginWynncraftVfxTintLayer(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
												 int packedLight, int packedOverlay, int color, CallbackInfo ci) {
		RenderType renderType = iris$candidateRenderType();
		if (!ImmediateState.captureItemEntityBatches
			|| !ImmediateState.capturePhotonTranslucentVfxPipelines
			|| renderType == null
			|| !ImmediateState.isWynncraftVfxCandidatePipeline(renderType.pipeline())) {
			return;
		}

		if (!iris$hasTranslucencyTint(tintLayers)) {
			return;
		}

		ImmediateState.beginForcedWynncraftItemLayerSignal(renderType.pipeline());
	}

	@Inject(method = "submit", at = @At("RETURN"))
	private void iris$endWynncraftVfxTintLayer(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
											  int packedLight, int packedOverlay, int color, CallbackInfo ci) {
		RenderType renderType = iris$candidateRenderType();
		if (ImmediateState.shouldForceCurrentItemLayerWynnSignal(renderType == null ? null : renderType.pipeline())) {
			ImmediateState.endForcedWynncraftItemLayerSignal();
		}
	}

	/**
	 * The RenderType that {@code ItemRenderer.renderItem} used to receive is no longer passed around:
	 * derive an equivalent from the layer's quads (preferring the translucent set, since the forced
	 * signal only ever targeted the translucent item pipeline).
	 */
	@Unique
	private RenderType iris$candidateRenderType() {
		ItemQuads itemQuads = this.quads;
		if (itemQuads == null || itemQuads.isEmpty()) {
			return null;
		}

		BakedQuad quad = null;
		if (!itemQuads.translucent().isEmpty()) {
			quad = itemQuads.translucent().get(0);
		} else if (!itemQuads.solid().isEmpty()) {
			quad = itemQuads.solid().get(0);
		}
		return quad == null ? null : quad.materialInfo().itemRenderType();
	}

	@Unique
	private static boolean iris$hasTranslucencyTint(IntList tintLayers) {
		if (tintLayers == null) {
			return false;
		}
		for (int tint : tintLayers.toIntArray()) {
			if (ImmediateState.isWynncraftTranslucencySignalArgb(tint)) {
				return true;
			}
		}
		return false;
	}
}
