package com.gali.ae2s_extended_crafting.init;

import com.gali.ae2s_extended_crafting.tile.CrafterCoreAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.EnderCrafterAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableAdvancedAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableBasicAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableEliteAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableUltimateAssemblyStationTile;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModTiles {
    private ModTiles() {
    }

    public static void register() {
        GameRegistry.registerTileEntity(TableBasicAssemblyStationTile.class,
                ModBlocks.id("table_basic_pattern_provider"));
        GameRegistry.registerTileEntity(TableAdvancedAssemblyStationTile.class,
                ModBlocks.id("table_advanced_pattern_provider"));
        GameRegistry.registerTileEntity(TableEliteAssemblyStationTile.class,
                ModBlocks.id("table_elite_pattern_provider"));
        GameRegistry.registerTileEntity(TableUltimateAssemblyStationTile.class,
                ModBlocks.id("table_ultimate_pattern_provider"));
        GameRegistry.registerTileEntity(CrafterCoreAssemblyStationTile.class,
                ModBlocks.id("crafter_core_pattern_provider"));
        GameRegistry.registerTileEntity(EnderCrafterAssemblyStationTile.class,
                ModBlocks.id("ender_crafter_pattern_provider"));
    }
}
