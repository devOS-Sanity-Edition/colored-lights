package dev.gegy.colored_lights.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.gegy.colored_lights.ColoredLightCorner;
import dev.gegy.colored_lights.ColoredLights;
import dev.gegy.colored_lights.mixin.render.chunk.BuiltChunkStorageAccess;
import dev.gegy.colored_lights.render.*;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = WorldRenderer.class, priority = 970)
public class WorldRendererMixin implements ColoredLightWorldRenderer, ColoredLightReader {
    @Shadow
    private ClientWorld world;
    @Shadow
    private BuiltChunkStorage chunks;

    private final ChunkLightColorUpdater chunkLightColorUpdater = new ChunkLightColorUpdater();

    private final BlockPos.Mutable readBlockPos = new BlockPos.Mutable();
    private GlUniform chunkLightColors;

    private long lastChunkLightColors;

    @Inject(method = "scheduleChunkRender", at = @At("HEAD"))
    private void scheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        this.chunkLightColorUpdater.rerenderChunk(this.world, this.chunks, x, y, z);
    }

    @Inject(method = "renderLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;startDrawing()V", shift = At.Shift.AFTER))
    private void prepareRenderLayer(RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, CallbackInfo ci) {
        var shader = RenderSystem.getShader();
        this.chunkLightColors = ColoredLights.CHUNK_LIGHT_COLORS.get(shader);
        this.lastChunkLightColors = 0;
    }

    @Inject(
            method = "renderLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlUniform;set(FFF)V")
    )
    private void prepareRenderChunk(RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, CallbackInfo ci, @Local ChunkBuilder.BuiltChunk builtChunk) {
        var chunkLightColors = this.chunkLightColors;
        if (chunkLightColors == null) return;

        long colors = ((ColoredLightBuiltChunk) builtChunk).getPackedChunkLightColors();
        if (this.lastChunkLightColors != colors) {
            this.lastChunkLightColors = colors;

            int colorsHigh = (int) (colors >>> 32);
            int colorsLow = (int) colors;
            chunkLightColors.set(colorsHigh, colorsLow);
            chunkLightColors.upload();
        }
    }

    @Inject(method = "renderLayer", at = @At("RETURN"))
    private void finishRenderLayer(RenderLayer layer, MatrixStack transform, double cameraX, double cameraY, double cameraZ, Matrix4f projection, CallbackInfo ci) {
        this.lastChunkLightColors = 0;

        var chunkLightColors = this.chunkLightColors;
        if (chunkLightColors != null) {
            chunkLightColors.set(0, 0);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        float skyBrightness = this.world.getSkyBrightness(tickDelta);
        ColoredLightEntityRenderContext.setGlobal(skyBrightness);
    }

    @Inject(
            method = "renderEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"),
            locals = LocalCapture.PRINT
    )
    private void beforeRenderEntity(
            Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci,
            @Local(ordinal = 3) double entityX, @Local(ordinal = 4) double entityY, @Local(ordinal = 5) double entityZ
    ) {
        this.read(entityX, entityY, entityZ, ColoredLightEntityRenderContext::set);
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void afterRenderEntity(
            Entity entity, double cameraX, double cameraY, double cameraZ,
            float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci
    ) {
        ColoredLightEntityRenderContext.end();
    }

    @Override
    public void read(double x, double y, double z, ColorConsumer consumer) {
        var readBlockPos = this.readBlockPos.set(x, y, z);
        var chunk = ((BuiltChunkStorageAccess) this.chunks).getBuiltChunk(readBlockPos);
        if (chunk == null) {
            return;
        }

        var corners = ((ColoredLightBuiltChunk) chunk).getChunkLightColors();
        if (corners != null) {
            BlockPos origin = chunk.getOrigin();
            float localX = (float) (x - origin.getX()) / 16.0F;
            float localY = (float) (y - origin.getY()) / 16.0F;
            float localZ = (float) (z - origin.getZ()) / 16.0F;
            ColoredLightCorner.mix(corners, localX, localY, localZ, consumer);
        }
    }

    @Override
    public ChunkLightColorUpdater getChunkLightColorUpdater() {
        return this.chunkLightColorUpdater;
    }
}
