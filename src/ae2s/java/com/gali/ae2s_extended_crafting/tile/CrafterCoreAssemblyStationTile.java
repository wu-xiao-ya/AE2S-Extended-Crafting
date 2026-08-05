package com.gali.ae2s_extended_crafting.tile;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import com.blakebr0.extendedcrafting.crafting.CombinationRecipe;
import com.gali.ae2s_extended_crafting.init.ModBlocks;
import com.gali.ae2s_extended_crafting.matcher.CombinationRecipeMatcher;
import com.gali.ae2s_extended_crafting.matcher.RecipeMatcher;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class CrafterCoreAssemblyStationTile extends AbstractAssemblyStationTile<CombinationRecipe> {
    private static final String NBT_PROCESSING_OUTPUTS = "coreProcessingOutputs";
    private static final String NBT_PROCESSING_PROGRESS = "coreProcessingProgress";
    private static final String NBT_PROCESSING_POWER_TOTAL = "coreProcessingPowerTotal";
    private static final String NBT_PROCESSING_POWER_RATE = "coreProcessingPowerRate";

    private final RecipeMatcher<CombinationRecipe> matcher = new CombinationRecipeMatcher();
    private final List<GenericStack> processingOutputs = new ArrayList<>();
    private double processingProgress;
    private long processingPowerTotal;
    private int processingPowerRate;

    @Override
    protected RecipeMatcher<CombinationRecipe> getRecipeMatcher() {
        return this.matcher;
    }

    @Override
    protected Block getStationBlock() {
        return ModBlocks.CRAFTER_CORE_PATTERN_PROVIDER.block();
    }

    @Override
    protected boolean isBusyForPush() {
        return super.isBusyForPush() || isProcessing();
    }

    @Override
    protected boolean pushMatchedPattern(IPatternDetails pattern, CombinationRecipe match, KeyCounter[] inputHolder,
                                         int multiplier) {
        if (isBusyForPush()) {
            return false;
        }

        long cost = Math.max(0, match.getCost());
        int rate = Math.max(0, match.getPerTick());
        if (cost <= 0 || rate <= 0) {
            return enqueueMatchedOutputs(pattern, match, multiplier);
        }

        this.processingOutputs.clear();
        this.processingOutputs.addAll(createMultipliedOutputs(pattern.getOutputs(), multiplier));
        this.processingProgress = 0;
        this.processingPowerTotal = saturatedMultiply(cost, Math.max(1, multiplier));
        this.processingPowerRate = getSpeedAdjustedPowerRate(rate);
        this.saveChanges();
        return true;
    }

    @Override
    public void serverTick() {
        if (isProcessing()) {
            processEnergyStep();
        }
        super.serverTick();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setTag(NBT_PROCESSING_OUTPUTS, GenericStack.writeList(this.processingOutputs));
        data.setDouble(NBT_PROCESSING_PROGRESS, this.processingProgress);
        data.setLong(NBT_PROCESSING_POWER_TOTAL, this.processingPowerTotal);
        data.setInteger(NBT_PROCESSING_POWER_RATE, this.processingPowerRate);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.processingOutputs.clear();
        for (GenericStack output : GenericStack.readList(data.getTagList(NBT_PROCESSING_OUTPUTS, 10))) {
            if (output != null) {
                this.processingOutputs.add(output);
            }
        }
        this.processingProgress = Math.max(0, data.getDouble(NBT_PROCESSING_PROGRESS));
        this.processingPowerTotal = Math.max(0, data.getLong(NBT_PROCESSING_POWER_TOTAL));
        this.processingPowerRate = Math.max(0, data.getInteger(NBT_PROCESSING_POWER_RATE));
        if (!isProcessing()) {
            clearProcessingState();
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        clearProcessingState();
    }

    private boolean isProcessing() {
        return this.processingPowerTotal > 0 && this.processingPowerRate > 0 && !this.processingOutputs.isEmpty();
    }

    private void processEnergyStep() {
        if (this.getMainNode().getGrid() == null) {
            return;
        }

        double remaining = this.processingPowerTotal - this.processingProgress;
        if (remaining <= 0) {
            finishProcessing();
            return;
        }

        double requested = Math.min(this.processingPowerRate, remaining);
        double extracted = this.getMainNode().getGrid().getEnergyService()
                .extractAEPower(requested, Actionable.MODULATE, PowerMultiplier.CONFIG);
        if (extracted <= 0.0001) {
            return;
        }

        this.processingProgress = Math.min(this.processingPowerTotal, this.processingProgress + extracted);
        if (this.processingProgress + 0.0001 >= this.processingPowerTotal) {
            finishProcessing();
        } else {
            this.saveChanges();
        }
    }

    private void finishProcessing() {
        List<GenericStack> completedOutputs = new ArrayList<>(this.processingOutputs);
        clearProcessingState();
        addOutputsToReturnInventory(completedOutputs);
        this.saveChanges();
    }

    private void clearProcessingState() {
        this.processingOutputs.clear();
        this.processingProgress = 0;
        this.processingPowerTotal = 0;
        this.processingPowerRate = 0;
    }

    private int getSpeedAdjustedPowerRate(int baseRate) {
        double adjusted = Math.ceil(Math.max(1, baseRate) * getSpeedMultiplier());
        return adjusted >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, (int) adjusted);
    }
}
