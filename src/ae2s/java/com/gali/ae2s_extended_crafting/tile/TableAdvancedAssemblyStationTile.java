package com.gali.ae2s_extended_crafting.tile;

import com.gali.ae2s_extended_crafting.init.ModBlocks;
import net.minecraft.block.Block;

public class TableAdvancedAssemblyStationTile extends AbstractTableAssemblyStationTile {
    public TableAdvancedAssemblyStationTile() {
        super(5);
    }

    @Override
    protected Block getStationBlock() {
        return ModBlocks.TABLE_ADVANCED_PATTERN_PROVIDER.block();
    }
}
