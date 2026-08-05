package com.gali.ae2s_extended_crafting.gui;

import ae2.client.gui.implementations.GuiPatternProvider;
import ae2.client.gui.style.GuiStyleManager;
import ae2.container.AEBaseContainer;
import ae2.container.implementations.ContainerPatternProvider;
import ae2.core.gui.PatternContainerGuiReturnContext;
import ae2.core.gui.locator.GuiHostLocators;
import com.gali.ae2s_extended_crafting.AE2SExtendedCrafting;
import com.gali.ae2s_extended_crafting.tile.AbstractAssemblyStationTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class ModGuiHandler implements IGuiHandler {
    private static final int PATTERN_PROVIDER = 0;
    private static final String PATTERN_PROVIDER_STYLE = "/screens/pattern_provider.json";

    public static void openPatternProvider(EntityPlayer player, AbstractAssemblyStationTile<?> tile) {
        BlockPos pos = tile.getPos();
        player.openGui(AE2SExtendedCrafting.INSTANCE, PATTERN_PROVIDER, tile.getWorld(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        AbstractAssemblyStationTile<?> tile = getAssemblyStation(id, world, x, y, z);
        if (tile == null) {
            return null;
        }

        return initTileContainer(new ContainerPatternProvider(player.inventory, tile), tile);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        AbstractAssemblyStationTile<?> tile = getAssemblyStation(id, world, x, y, z);
        if (tile == null) {
            return null;
        }

        ContainerPatternProvider container = initTileContainer(
                new ContainerPatternProvider(player.inventory, tile),
                tile
        );
        return new GuiPatternProvider(
                container,
                player.inventory,
                getAssemblyStationTitle(tile),
                GuiStyleManager.loadStyleDoc(PATTERN_PROVIDER_STYLE)
        );
    }

    private static AbstractAssemblyStationTile<?> getAssemblyStation(int id, World world, int x, int y, int z) {
        if (id != PATTERN_PROVIDER || world == null) {
            return null;
        }

        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (tile instanceof AbstractAssemblyStationTile) {
            return (AbstractAssemblyStationTile<?>) tile;
        }
        return null;
    }

    private static ITextComponent getAssemblyStationTitle(AbstractAssemblyStationTile<?> tile) {
        World world = tile.getWorld();
        if (world == null || tile.getPos() == null) {
            return null;
        }

        IBlockState state = world.getBlockState(tile.getPos());
        if (state == null || state.getBlock() == null || state.getBlock().getRegistryName() == null) {
            return null;
        }

        ResourceLocation id = state.getBlock().getRegistryName();
        return new TextComponentTranslation("tile." + id.getNamespace() + "." + id.getPath() + ".name");
    }

    private static <C extends AEBaseContainer> C initTileContainer(C container, TileEntity tile) {
        container.setLocator(GuiHostLocators.forTile(tile));
        container.setReturnedFromSubScreen(false);
        return PatternContainerGuiReturnContext.initializeContainer(container);
    }
}
