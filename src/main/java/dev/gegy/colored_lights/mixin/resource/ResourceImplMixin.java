package dev.gegy.colored_lights.mixin.resource;

import dev.gegy.colored_lights.resource.ResourceIdDuck;
import dev.gegy.colored_lights.resource.ResourcePatchManager;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(Resource.class)
public class ResourceImplMixin implements ResourceIdDuck {
    @Shadow @Final @Mutable private InputSupplier<InputStream> inputSupplier;
    @Shadow @Nullable private ResourceMetadata metadata;
    @Shadow @Final private ResourcePack pack;
    @Unique
    private boolean colored_lights$patchedResource;

    @Inject(method = "getInputStream", at = @At("HEAD"))
    private void getInputStream(CallbackInfoReturnable<InputStream> ci) {
        if (!this.colored_lights$patchedResource) {
            this.colored_lights$patchedResource = true;
            var original = this.inputSupplier;
            this.inputSupplier = () -> ResourcePatchManager.INSTANCE.patch(this.id, original.get());
        }
    }

    @Unique
    private Identifier id;

    @Override
    public Identifier colored_lights$getId() {
        return this.id;
    }

    @Override
    public void colored_lights$setId(Identifier id) {
        this.id = id;
    }
}
