package com.gali.ae2s_extended_crafting.tile;

import com.gali.ae2s_extended_crafting.init.ModBlocks;
import net.minecraft.block.Block;

public class TableEliteAssemblyStationTile extends AbstractTableAssemblyStationTile {
    public TableEliteAssemblyStationTile() {
        super(7);
    }

    @Override
    protected Block getStationBlock() {
        return ModBlocks.TABLE_ELITE_PATTERN_PROVIDER.block();
    }
}
