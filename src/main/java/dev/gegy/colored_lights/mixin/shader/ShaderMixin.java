package dev.gegy.colored_lights.mixin.shader;

import dev.gegy.colored_lights.resource.shader.PatchedShader;
import dev.gegy.colored_lights.resource.shader.PatchedUniform;
import dev.gegy.colored_lights.resource.shader.ShaderPatchManager;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ShaderProgram.class)
public abstract class ShaderMixin implements PatchedShader {
    @Shadow @Final private List<GlUniform> uniforms;

    private final Map<PatchedUniform, GlUniform> patchedUniforms = new Reference2ObjectOpenHashMap<>();

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceFactory;openAsReader(Lnet/minecraft/util/Identifier;)Ljava/io/BufferedReader;"))
    private void initEarly(ResourceFactory factory, String name, VertexFormat format, CallbackInfo ci) {
        ShaderPatchManager.startPatching(name);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;readBlendState(Lcom/google/gson/JsonObject;)Lnet/minecraft/client/gl/GlBlendState;"))
    private void initUniforms(ResourceFactory factory, String name, VertexFormat format, CallbackInfo ci) {
        ShaderPatchManager.applyUniformPatches((ShaderProgram) (Object) this, (patchedUniform, glUniform) -> {
            this.uniforms.add(glUniform);
            this.patchedUniforms.put(patchedUniform, glUniform);
        });
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initLate(ResourceFactory factory, String name, VertexFormat format, CallbackInfo ci) {
        ShaderPatchManager.stopPatching();
    }

    @Override
    public @Nullable GlUniform getPatchedUniform(PatchedUniform uniform) {
        return this.patchedUniforms.get(uniform);
    }
}
