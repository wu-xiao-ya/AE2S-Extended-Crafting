package com.gali.ae2s_extended_crafting;

import com.gali.ae2s_extended_crafting.init.ModBlocks;
import com.gali.ae2s_extended_crafting.init.ModTiles;
import com.gali.ae2s_extended_crafting.init.ModUpgrades;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(
        modid = AE2SExtendedCrafting.MODID,
        name = AE2SExtendedCrafting.NAME,
        version = AE2SExtendedCrafting.VERSION,
        dependencies = "required-after:ae2;required-after:extendedcrafting"
)
@Mod.EventBusSubscriber
public final class AE2SExtendedCrafting {
    public static final String MODID = "ae2s_extended_crafting";
    public static final String NAME = "AE2S Extended Crafting";
    public static final String VERSION = "0.1.0-dev";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModUpgrades.register();
        ModTiles.register();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ModBlocks.registerBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ModBlocks.registerItems(event.getRegistry());
    }
}
