package dev.gegy.colored_lights.mixin.resource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.gegy.colored_lights.resource.ResourceIdDuck;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderProgram.class)
public class ShaderProgramMixin {
    @WrapOperation(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceFactory;getResourceOrThrow(Lnet/minecraft/util/Identifier;)Lnet/minecraft/resource/Resource;"))
    private static Resource colored_lights$addResourceId(ResourceFactory instance, Identifier id, Operation<Resource> original) {
        var resource = original.call(instance, id);
        var duck = ((ResourceIdDuck) resource);

        if (duck.colored_lights$getId() == null)
            duck.colored_lights$setId(id);

        return resource;
    }
}
