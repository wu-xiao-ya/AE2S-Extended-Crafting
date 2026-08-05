package com.gali.ae2s_extended_crafting.init;

import ae2.core.definitions.BlockDefinition;
import com.gali.ae2s_extended_crafting.AE2SExtendedCrafting;
import com.gali.ae2s_extended_crafting.block.AssemblyStationBlock;
import com.gali.ae2s_extended_crafting.tile.AbstractAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.CrafterCoreAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableAdvancedAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableBasicAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableEliteAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableUltimateAssemblyStationTile;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

public final class ModBlocks {
    public static final BlockDefinition<AssemblyStationBlock<TableBasicAssemblyStationTile>> TABLE_BASIC_PATTERN_PROVIDER =
            table("Basic Table Assembly Station", "table_basic_pattern_provider", TableBasicAssemblyStationTile.class);
    public static final BlockDefinition<AssemblyStationBlock<TableAdvancedAssemblyStationTile>> TABLE_ADVANCED_PATTERN_PROVIDER =
            table("Advanced Table Assembly Station", "table_advanced_pattern_provider", TableAdvancedAssemblyStationTile.class);
    public static final BlockDefinition<AssemblyStationBlock<TableEliteAssemblyStationTile>> TABLE_ELITE_PATTERN_PROVIDER =
            table("Elite Table Assembly Station", "table_elite_pattern_provider", TableEliteAssemblyStationTile.class);
    public static final BlockDefinition<AssemblyStationBlock<TableUltimateAssemblyStationTile>> TABLE_ULTIMATE_PATTERN_PROVIDER =
            table("Ultimate Table Assembly Station", "table_ultimate_pattern_provider", TableUltimateAssemblyStationTile.class);
    public static final BlockDefinition<AssemblyStationBlock<CrafterCoreAssemblyStationTile>> CRAFTER_CORE_PATTERN_PROVIDER =
            table("Crafting Core Assembly Station", "crafter_core_pattern_provider", CrafterCoreAssemblyStationTile.class);

    private static final BlockDefinition<?>[] ALL = new BlockDefinition[]{
            TABLE_BASIC_PATTERN_PROVIDER,
            TABLE_ADVANCED_PATTERN_PROVIDER,
            TABLE_ELITE_PATTERN_PROVIDER,
            TABLE_ULTIMATE_PATTERN_PROVIDER,
            CRAFTER_CORE_PATTERN_PROVIDER
    };

    private ModBlocks() {
    }

    private static <T extends AbstractAssemblyStationTile<?>> BlockDefinition<AssemblyStationBlock<T>> table(
            String englishName, String id, Class<T> tileClass) {
        return new BlockDefinition<>(englishName, id(id), new AssemblyStationBlock<>(tileClass));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(AE2SExtendedCrafting.MODID, path);
    }

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        for (BlockDefinition<?> definition : ALL) {
            registry.register(definition.block());
        }
    }

    public static void registerItems(IForgeRegistry<Item> registry) {
        for (BlockDefinition<?> definition : ALL) {
            if (definition.item() != null) {
                registry.register(definition.item());
            }
        }
    }
}
