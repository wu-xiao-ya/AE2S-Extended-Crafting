/*
 * Item rendering adapted from Applied Energistics 2 / AE2S MolecularAssemblerTESR.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package com.gali.ae2s_extended_crafting.client;

import com.gali.ae2s_extended_crafting.tile.AbstractTableAssemblyStationTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TableAssemblyStationRenderer extends TileEntitySpecialRenderer<AbstractTableAssemblyStationTile> {
    @Override
    public void render(AbstractTableAssemblyStationTile tile, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity camera = minecraft.getRenderViewEntity();
        if (destroyStage >= 0 || camera == null
                || camera.getDistanceSqToCenter(tile.getPos())
                > AbstractTableAssemblyStationTile.ITEM_RENDER_DISTANCE_SQUARED) {
            return;
        }
        ItemStack stack = tile.getCraftingDisplayStack();
        if (stack.isEmpty()) {
            return;
        }

        // Follow the upstream TESR lighting convention; always unwind our matrix.
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
            if (stack.getItem() instanceof ItemBlock) {
                GlStateManager.translate(0.0F, -0.2F, 0.0F);
            } else {
                GlStateManager.translate(0.0F, -0.3F, 0.0F);
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
            }
            RenderHelper.enableStandardItemLighting();
            minecraft.getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.GROUND);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
