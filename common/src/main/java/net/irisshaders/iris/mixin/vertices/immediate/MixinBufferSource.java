package net.irisshaders.iris.mixin.vertices.immediate;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quick optimization to disable the extended vertex format outside of level rendering.
 * This is a heuristic that should hopefully work almost always because of how the buffer sources are used.
 * <p>
 * 26.3: {@code net.minecraft.client.renderer.MultiBufferSource} (and its immediate-mode
 * {@code BufferSource} path) were removed. The equivalent staging path is now
 * {@link net.minecraft.client.renderer.StagedVertexBuffer}, so the mixin is retargeted to it:
 * {@code getVertexBuilder(Draw)} replaces {@code BufferSource#getBuffer}, and {@code upload()} replaces
 * {@code endBatch}. The WynnCraft translucent entity deferral that used to live here depended on the
 * removed {@code endBatch(RenderType, BufferBuilder)} / {@code RenderType.draw(MeshData)} hooks; a
 * {@link net.minecraft.client.renderer.StagedVertexBuffer.Draw} carries no RenderType, so that deferral
 * cannot be reconstructed here and has been dropped. The capture flags
 * ({@link ImmediateState#captureItemEntityBatches} etc.) are still set/read by other mixins but no longer
 * trigger any draw deferral.
 */
@Mixin(StagedVertexBuffer.class)
public class MixinBufferSource {
	@WrapMethod(method = "getVertexBuilder")
	private VertexConsumer iris$redirectBegin(StagedVertexBuffer.Draw draw, Operation<VertexConsumer> original) {
		ImmediateState.skipExtension.set(iris$notRenderingLevel());
		VertexConsumer builder = original.call(draw);
		ImmediateState.skipExtension.set(false);

		return builder;
	}

	@Inject(method = "upload",
		at = @At(value = "HEAD"))
	private void iris$beforeFlushBuffer(CallbackInfo ci) {
		if (iris$notRenderingLevel()) {
			ImmediateState.renderWithExtendedVertexFormat = false;
		}
	}

	@Inject(method = "upload",
		at = @At(value = "RETURN"))
	private void iris$afterFlushBuffer(CallbackInfo ci) {
		if (iris$notRenderingLevel()) {
			ImmediateState.renderWithExtendedVertexFormat = true;
		}
	}

	@Unique
	private boolean iris$notRenderingLevel() {
		return !ImmediateState.isRenderingLevel;
	}
}
