package com.jarrettonesource.createmekanismcompat.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;
import mekanism.common.content.miner.MinerFilter;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TileEntityDigitalMiner.class, remap = false)
public interface TileEntityDigitalMinerAccessor {
    @Accessor("running")
    void cmc$setRunning(boolean running);

    @Accessor("oresToMine")
    void cmc$setOresToMine(Long2ObjectMap<BitSet> oresToMine);

    @Accessor("cachedToMine")
    void cmc$setCachedToMine(int cachedToMine);

    @Accessor("missingStack")
    void cmc$setMissingStack(ItemStack missingStack);

    @Accessor("overflowCollector")
    Predicate<ItemStack> cmc$getOverflowCollector();

    @Invoker("tryMineBlock")
    void cmc$invokeTryMineBlock();

    @Invoker("canMine")
    boolean cmc$canMine(BlockState state, BlockPos pos);

    @Invoker("getDrops")
    List<ItemStack> cmc$getDrops(ServerLevel level, BlockState state, BlockPos pos);

    @Invoker("setReplace")
    boolean cmc$setReplace(BlockState state, BlockPos pos, @Nullable MinerFilter<?> filter);

    @Invoker("add")
    void cmc$add(List<ItemStack> stacks);

    @Invoker("tryAddOverflow")
    void cmc$tryAddOverflow();
}
