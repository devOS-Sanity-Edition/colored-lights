package dev.gegy.colored_lights.provider;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public interface BlockLightColorProvider {
    @Nullable
    Vector3f get(WorldView world, BlockPos pos, BlockState state);
}
