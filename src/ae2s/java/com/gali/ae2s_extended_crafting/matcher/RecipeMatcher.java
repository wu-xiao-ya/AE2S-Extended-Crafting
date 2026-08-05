package com.gali.ae2s_extended_crafting.matcher;

import ae2.api.crafting.IPatternDetails;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.World;

public interface RecipeMatcher<M> {
    @Nullable
    M match(IPatternDetails pattern, World world);
}
