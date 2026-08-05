package com.gali.ae2s_extended_crafting.tile;

import com.gali.ae2s_extended_crafting.matcher.RecipeMatcher;
import com.gali.ae2s_extended_crafting.matcher.TableRecipeMatcher;
import net.minecraft.item.crafting.IRecipe;

public abstract class AbstractTableAssemblyStationTile extends AbstractAssemblyStationTile<IRecipe> {
    private final RecipeMatcher<IRecipe> matcher;

    protected AbstractTableAssemblyStationTile(int lineSize) {
        this.matcher = new TableRecipeMatcher(lineSize);
    }

    @Override
    protected RecipeMatcher<IRecipe> getRecipeMatcher() {
        return this.matcher;
    }
}
