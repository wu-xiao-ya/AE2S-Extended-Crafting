package com.gali.ae2s_extended_crafting.tile;

import com.gali.ae2s_extended_crafting.init.ModBlocks;
import net.minecraft.block.Block;

public class TableBasicAssemblyStationTile extends AbstractTableAssemblyStationTile {
    public TableBasicAssemblyStationTile() {
        super(3);
    }

    @Override
    protected Block getStationBlock() {
        return ModBlocks.TABLE_BASIC_PATTERN_PROVIDER.block();
    }
}
