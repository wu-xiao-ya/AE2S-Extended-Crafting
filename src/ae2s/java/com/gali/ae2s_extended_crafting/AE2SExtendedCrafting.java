package com.gali.ae2s_extended_crafting;

import com.gali.ae2s_extended_crafting.init.ModBlocks;
import com.gali.ae2s_extended_crafting.init.ModTiles;
import com.gali.ae2s_extended_crafting.init.ModUpgrades;
import com.gali.ae2s_extended_crafting.gui.ModGuiHandler;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(
        modid = AE2SExtendedCrafting.MODID,
        name = AE2SExtendedCrafting.NAME,
        version = AE2SExtendedCrafting.VERSION,
        dependencies = "required-after:ae2@[1.1.0,);required-after:extendedcrafting"
)
@Mod.EventBusSubscriber
public final class AE2SExtendedCrafting {
    public static final String MODID = "ae2s_extended_crafting";
    public static final String NAME = "AE2S Extended Crafting";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MODID)
    public static AE2SExtendedCrafting INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
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
