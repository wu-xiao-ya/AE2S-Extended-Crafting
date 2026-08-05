package com.gali.ae2s_extended_crafting.block;

import ae2.api.networking.extensions.GridLogicExtensions;
import ae2.block.crafting.PatternProviderBlock;
import ae2.block.crafting.PushDirection;
import ae2.util.InteractionUtil;
import ae2.util.Platform;
import com.gali.ae2s_extended_crafting.gui.ModGuiHandler;
import com.gali.ae2s_extended_crafting.tile.AbstractAssemblyStationTile;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class AssemblyStationBlock<T extends AbstractAssemblyStationTile<?>> extends Block implements ITileEntityProvider {
    private static final PushDirection[] PUSH_DIRECTIONS = PushDirection.values();
    public static final PropertyEnum<PushDirection> PUSH_DIRECTION = PatternProviderBlock.PUSH_DIRECTION;

    private final Class<T> tileClass;

    public AssemblyStationBlock(Class<T> tileClass) {
        super(Material.IRON);
        this.tileClass = tileClass;
        this.setHardness(2.2F);
        this.setResistance(11.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(PUSH_DIRECTION, PushDirection.ALL));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        try {
            return this.tileClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create tile entity " + this.tileClass, e);
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PUSH_DIRECTION);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(PUSH_DIRECTION).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int safeMeta = meta >= 0 && meta < PUSH_DIRECTIONS.length ? meta : PushDirection.ALL.ordinal();
        return this.getDefaultState().withProperty(PUSH_DIRECTION, PUSH_DIRECTIONS[safeMeta]);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        T tile = this.getAssemblyStation(world, pos);
        if (tile == null) {
            return;
        }

        tile.getLogic().invalidateTargetCaches();
        tile.getLogic().updateRedstoneState();
        EnumFacing neighborSide = GridLogicExtensions.getNeighborSide(pos, fromPos);
        if (neighborSide != null && !world.isRemote) {
            tile.getLogic().onNeighborChanged(neighborSide);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ)) {
            return true;
        }

        T tile = this.getAssemblyStation(world, pos);
        if (tile == null) {
            return false;
        }

        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && InteractionUtil.canWrenchRotate(player, held, pos)) {
            if (!world.isRemote) {
                setSide(world, pos, facing);
            }
            return true;
        }

        if (!world.isRemote) {
            ModGuiHandler.openPatternProvider(player, tile);
        }
        return true;
    }

    private void setSide(World world, BlockPos pos, EnumFacing side) {
        IBlockState state = world.getBlockState(pos);
        EnumFacing currentSide = state.getValue(PUSH_DIRECTION).getDirection();
        PushDirection nextSide;
        if (currentSide == side.getOpposite()) {
            nextSide = PushDirection.fromDirection(side);
        } else if (currentSide == side) {
            nextSide = PushDirection.ALL;
        } else if (currentSide == null) {
            nextSide = PushDirection.fromDirection(side.getOpposite());
        } else {
            nextSide = PushDirection.fromDirection(Platform.rotateAround(currentSide, side));
        }

        world.setBlockState(pos, state.withProperty(PUSH_DIRECTION, nextSide), 3);
        T tile = this.getAssemblyStation(world, pos);
        if (tile != null) {
            tile.getLogic().invalidateTargetCaches();
            tile.onPushDirectionChanged();
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        T tile = this.getAssemblyStation(world, pos);
        if (tile != null && !world.isRemote) {
            List<ItemStack> drops = new ArrayList<>();
            tile.addAdditionalDrops(drops);
            tile.clearContent();
            Platform.spawnDrops(world, pos, drops);
        }
        super.breakBlock(world, pos, state);
    }

    private T getAssemblyStation(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (this.tileClass.isInstance(tile)) {
            return this.tileClass.cast(tile);
        }
        return null;
    }
}
