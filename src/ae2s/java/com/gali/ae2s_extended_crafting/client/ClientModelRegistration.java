package com.gali.ae2s_extended_crafting.client;

import ae2.core.definitions.BlockDefinition;
import com.gali.ae2s_extended_crafting.AE2SExtendedCrafting;
import com.gali.ae2s_extended_crafting.init.ModBlocks;
import com.gali.ae2s_extended_crafting.tile.TableBasicAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableAdvancedAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableEliteAssemblyStationTile;
import com.gali.ae2s_extended_crafting.tile.TableUltimateAssemblyStationTile;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.item.Item;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AE2SExtendedCrafting.MODID, value = Side.CLIENT)
public final class ClientModelRegistration {
    private ClientModelRegistration() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        TableAssemblyStationRenderer renderer = new TableAssemblyStationRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(TableBasicAssemblyStationTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(TableAdvancedAssemblyStationTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(TableEliteAssemblyStationTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(TableUltimateAssemblyStationTile.class, renderer);
        registerBlockItem(ModBlocks.TABLE_BASIC_PATTERN_PROVIDER);
        registerBlockItem(ModBlocks.TABLE_ADVANCED_PATTERN_PROVIDER);
        registerBlockItem(ModBlocks.TABLE_ELITE_PATTERN_PROVIDER);
        registerBlockItem(ModBlocks.TABLE_ULTIMATE_PATTERN_PROVIDER);
        registerBlockItem(ModBlocks.CRAFTER_CORE_PATTERN_PROVIDER);
        registerBlockItem(ModBlocks.ENDER_CRAFTER_PATTERN_PROVIDER);
    }

    private static void registerBlockItem(BlockDefinition<?> definition) {
        Item item = definition.item();
        if (item == null || item.getRegistryName() == null) {
            return;
        }

        ModelResourceLocation model = new ModelResourceLocation(item.getRegistryName(), "inventory");
        for (int meta = 0; meta < 16; meta++) {
            ModelLoader.setCustomModelResourceLocation(item, meta, model);
        }
    }
}
