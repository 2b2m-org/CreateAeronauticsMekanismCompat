package com.jarrettonesource.createmekanismcompat.mixin.client;

import mekanism.common.block.BlockBounding;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockBounding.class, remap = false)
public abstract class BlockBoundingMixin {
    @Redirect(
            method = "proxyShape",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState cmc$getRenderableMainBlockState(BlockGetter level, BlockPos mainPos) {
        try {
            return level.getBlockState(mainPos);
        } catch (IndexOutOfBoundsException exception) {
            if (level instanceof RenderChunkRegion) {
                return Blocks.AIR.defaultBlockState();
            }
            throw exception;
        }
    }
}
