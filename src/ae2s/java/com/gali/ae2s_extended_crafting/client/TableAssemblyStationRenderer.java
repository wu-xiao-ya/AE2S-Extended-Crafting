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

import ae2.client.render.effects.ParticleTypes;
import ae2.core.AppEngBase;
import com.gali.ae2s_extended_crafting.tile.AbstractTableAssemblyStationTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.WeakHashMap;

@SideOnly(Side.CLIENT)
public final class TableAssemblyStationRenderer extends TileEntitySpecialRenderer<AbstractTableAssemblyStationTile> {
    // Weak keys do not retain unloaded tiles/worlds; never emit once per rendered frame.
    private final Map<AbstractTableAssemblyStationTile, Long> lastParticleTick = new WeakHashMap<>();

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
            this.lastParticleTick.remove(tile);
            return;
        }
        if (!minecraft.isGamePaused()) {
            spawnCraftingParticles(tile);
        }

        // Follow the upstream TESR lighting convention; always unwind our matrix.
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
            float animationTicks = (tile.getWorld().getTotalWorldTime() % 90L)
                    + (minecraft.isGamePaused() ? 0.0F : partialTicks);
            GlStateManager.rotate(animationTicks * 4.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            RenderHelper.enableStandardItemLighting();
            // NONE avoids the item's GROUND translation shifting it below the cage centre.
            minecraft.getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void spawnCraftingParticles(AbstractTableAssemblyStationTile tile) {
        long now = tile.getWorld().getTotalWorldTime();
        Long lastTick = this.lastParticleTick.get(tile);
        if (lastTick != null && now >= lastTick && now - lastTick < 4L) {
            return;
        }
        this.lastParticleTick.put(tile, now);
        if (!AppEngBase.runtime().shouldAddParticles(tile.getWorld().rand)) {
            return;
        }
        // Same inward-moving CRAFTING effect as AE2S; fixed budget regardless of parallelism.
        for (int i = 0; i < 2; i++) {
            double dx = (tile.getWorld().rand.nextDouble() - 0.5) * 0.7;
            double dy = (tile.getWorld().rand.nextDouble() - 0.5) * 0.7;
            double dz = (tile.getWorld().rand.nextDouble() - 0.5) * 0.7;
            ParticleTypes.CRAFTING.spawn(tile.getWorld(),
                    tile.getPos().getX() + 0.5 + dx,
                    tile.getPos().getY() + 0.5 + dy,
                    tile.getPos().getZ() + 0.5 + dz,
                    -dx * 0.2, -dy * 0.2, -dz * 0.2, null);
        }
    }
}
