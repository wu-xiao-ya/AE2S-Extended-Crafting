package com.gali.ae2s_extended_crafting.compat.jei;

import com.blakebr0.extendedcrafting.compat.jei.combinationcrafting.CombinationCraftingCategory;
import com.blakebr0.extendedcrafting.compat.jei.endercrafter.EnderCrafterCategory;
import com.blakebr0.extendedcrafting.compat.jei.tablecrafting.AdvancedTableCategory;
import com.blakebr0.extendedcrafting.compat.jei.tablecrafting.BasicTableCategory;
import com.blakebr0.extendedcrafting.compat.jei.tablecrafting.EliteTableCategory;
import com.blakebr0.extendedcrafting.compat.jei.tablecrafting.UltimateTableCategory;
import com.blakebr0.extendedcrafting.config.ModConfig;
import com.gali.ae2s_extended_crafting.init.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.item.ItemStack;

/**
 * Discovered by JEI (or its HEI-compatible plugin loader), never by common mod initialization.
 * Only adds machine catalysts to Extended Crafting's existing recipe categories.
 */
@JEIPlugin
public final class AssemblyStationJeiPlugin implements IModPlugin {
    @Override
    public void register(IModRegistry registry) {
        if (ModConfig.confTableEnabled) {
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.TABLE_BASIC_PATTERN_PROVIDER.block()),
                    BasicTableCategory.UID);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.TABLE_ADVANCED_PATTERN_PROVIDER.block()),
                    AdvancedTableCategory.UID);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.TABLE_ELITE_PATTERN_PROVIDER.block()),
                    EliteTableCategory.UID);
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.TABLE_ULTIMATE_PATTERN_PROVIDER.block()),
                    UltimateTableCategory.UID);
        }
        if (ModConfig.confCraftingCoreEnabled) {
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.CRAFTER_CORE_PATTERN_PROVIDER.block()),
                    CombinationCraftingCategory.UID);
        }
        if (ModConfig.confEnderEnabled) {
            registry.addRecipeCatalyst(new ItemStack(ModBlocks.ENDER_CRAFTER_PATTERN_PROVIDER.block()),
                    EnderCrafterCategory.UID);
        }
    }
}
