package com.gali.ae2s_extended_crafting.tile;

import com.gali.ae2s_extended_crafting.init.ModBlocks;
import net.minecraft.block.Block;

public class TableUltimateAssemblyStationTile extends AbstractTableAssemblyStationTile {
    public TableUltimateAssemblyStationTile() {
        super(9);
    }

    @Override
    protected Block getStationBlock() {
        return ModBlocks.TABLE_ULTIMATE_PATTERN_PROVIDER.block();
    }
}
