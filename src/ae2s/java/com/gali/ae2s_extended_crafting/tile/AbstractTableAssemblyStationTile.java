package com.gali.ae2s_extended_crafting.tile;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.GenericStack;
import com.gali.ae2s_extended_crafting.matcher.RecipeMatcher;
import com.gali.ae2s_extended_crafting.matcher.TableRecipeMatcher;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.List;

public abstract class AbstractTableAssemblyStationTile extends AbstractAssemblyStationTile<IRecipe> {
    public static final double ITEM_RENDER_DISTANCE_SQUARED = 32.0 * 32.0;
    private static final int DISPLAY_TICKS = 10;

    private final RecipeMatcher<IRecipe> matcher;
    // Transient visual state only: never persisted or used to produce actual outputs.
    private ItemStack displayStack = ItemStack.EMPTY;
    private long displayUntil;
    private boolean visualUpdatePending;

    protected AbstractTableAssemblyStationTile(int lineSize) {
        this.matcher = new TableRecipeMatcher(lineSize);
    }

    @Override
    protected RecipeMatcher<IRecipe> getRecipeMatcher() {
        return this.matcher;
    }

    @Override
    protected boolean enqueueMatchedOutputs(IPatternDetails pattern, IRecipe match, int multiplier) {
        if (!super.enqueueMatchedOutputs(pattern, match, multiplier)) {
            return false;
        }
        List<GenericStack> outputs = pattern.getOutputs();
        this.displayStack = outputs.isEmpty() ? ItemStack.EMPTY
                : outputs.get(0).what().wrapForDisplayOrFilter().copy();
        if (!this.displayStack.isEmpty()) {
            this.displayStack.setCount(1);
        }
        this.displayUntil = this.getWorld().getTotalWorldTime() + DISPLAY_TICKS;
        this.visualUpdatePending = true;
        return true;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!this.displayStack.isEmpty() && this.getWorld().getTotalWorldTime() >= this.displayUntil) {
            this.displayStack = ItemStack.EMPTY;
            this.displayUntil = 0;
            this.visualUpdatePending = true;
        }
        if (this.visualUpdatePending) {
            this.visualUpdatePending = false;
            this.markForUpdate();
        }
    }

    public ItemStack getCraftingDisplayStack() {
        return this.getWorld() != null && this.getWorld().getTotalWorldTime() < this.displayUntil
                ? this.displayStack : ItemStack.EMPTY;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return ITEM_RENDER_DISTANCE_SQUARED;
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        PacketBuffer packet = new PacketBuffer(data);
        packet.writeItemStack(this.getCraftingDisplayStack());
        packet.writeLong(this.displayUntil);
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        PacketBuffer packet = new PacketBuffer(data);
        try {
            ItemStack stack = packet.readItemStack();
            long until = packet.readLong();
            changed |= !ItemStack.areItemStacksEqual(this.displayStack, stack) || this.displayUntil != until;
            this.displayStack = stack;
            this.displayUntil = until;
            return changed;
        } catch (IOException e) {
            throw new DecoderException("Failed to read assembly station display stack", e);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.displayStack = ItemStack.EMPTY;
        this.displayUntil = 0;
        this.visualUpdatePending = true;
    }
}
