package com.gali.ae2s_extended_crafting.tile;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import com.blakebr0.extendedcrafting.config.ModConfig;
import com.blakebr0.extendedcrafting.crafting.endercrafter.IEnderCraftingRecipe;
import com.gali.ae2s_extended_crafting.init.ModBlocks;
import com.gali.ae2s_extended_crafting.matcher.EnderCrafterRecipeMatcher;
import com.gali.ae2s_extended_crafting.matcher.RecipeMatcher;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class EnderCrafterAssemblyStationTile extends AbstractAssemblyStationTile<IEnderCraftingRecipe> {
    private static final String NBT_PROCESSING_OUTPUTS = "enderProcessingOutputs";
    private static final String NBT_PROCESSING_TIME = "enderProcessingTime";
    private static final String NBT_PROCESSING_TIME_TOTAL = "enderProcessingTimeTotal";
    private static final String NBT_PROCESSING_RECIPE_SECONDS = "enderProcessingRecipeSeconds";
    private static final String NBT_PROCESSING_MULTIPLIER = "enderProcessingMultiplier";
    private static final ResourceLocation ENDER_ALTERNATOR_ID =
            new ResourceLocation("extendedcrafting", "ender_alternator");
    private static final int ALTERNATOR_SCAN_RADIUS = 3;
    private static final int INSTANT_OUTPUT_ALTERNATOR_THRESHOLD = 147;
    private static final int MINIMUM_PROCESSING_TICKS = 20;

    private final RecipeMatcher<IEnderCraftingRecipe> matcher = new EnderCrafterRecipeMatcher();
    private final List<GenericStack> processingOutputs = new ArrayList<>();
    private int processingTime;
    private int processingTimeTotal;
    private int processingRecipeSeconds;
    private int processingMultiplier = 1;

    @Override
    protected RecipeMatcher<IEnderCraftingRecipe> getRecipeMatcher() {
        return this.matcher;
    }

    @Override
    protected Block getStationBlock() {
        return ModBlocks.ENDER_CRAFTER_PATTERN_PROVIDER.block();
    }

    @Override
    protected boolean isBusyForPush() {
        return super.isBusyForPush() || (!shouldOutputInstantly(getAlternatorCount()) && isProcessing());
    }

    @Override
    protected boolean pushMatchedPattern(IPatternDetails pattern, IEnderCraftingRecipe match, KeyCounter[] inputHolder,
                                         int multiplier) {
        int alternatorCount = getAlternatorCount();
        if (shouldOutputInstantly(alternatorCount)) {
            finishProcessingImmediately();
            return enqueueMatchedOutputs(pattern, match, multiplier);
        }
        if (alternatorCount <= 0 || isProcessing()) {
            return false;
        }

        List<GenericStack> outputs = createMultipliedOutputs(pattern.getOutputs(), multiplier);
        if (outputs.isEmpty()) {
            return false;
        }

        this.processingOutputs.clear();
        this.processingOutputs.addAll(outputs);
        this.processingRecipeSeconds = getRecipeSeconds(match);
        this.processingMultiplier = Math.max(1, multiplier);
        this.processingTimeTotal = getProcessingDurationTicks(alternatorCount);
        this.processingTime = this.processingTimeTotal;
        this.saveChanges();
        return true;
    }

    @Override
    public void serverTick() {
        if (isProcessing()) {
            processTimeStep();
        }
        super.serverTick();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setTag(NBT_PROCESSING_OUTPUTS, GenericStack.writeList(this.processingOutputs));
        data.setInteger(NBT_PROCESSING_TIME, this.processingTime);
        data.setInteger(NBT_PROCESSING_TIME_TOTAL, this.processingTimeTotal);
        data.setInteger(NBT_PROCESSING_RECIPE_SECONDS, this.processingRecipeSeconds);
        data.setInteger(NBT_PROCESSING_MULTIPLIER, this.processingMultiplier);
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
        this.processingTime = Math.max(0, data.getInteger(NBT_PROCESSING_TIME));
        this.processingTimeTotal = Math.max(0, data.getInteger(NBT_PROCESSING_TIME_TOTAL));
        this.processingRecipeSeconds = Math.max(0, data.getInteger(NBT_PROCESSING_RECIPE_SECONDS));
        this.processingMultiplier = Math.max(1, data.getInteger(NBT_PROCESSING_MULTIPLIER));
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
        return this.processingTime > 0
                && this.processingTimeTotal > 0
                && this.processingRecipeSeconds > 0
                && !this.processingOutputs.isEmpty();
    }

    private void processTimeStep() {
        int alternatorCount = getAlternatorCount();
        if (shouldOutputInstantly(alternatorCount)) {
            finishProcessingImmediately();
            return;
        }
        if (alternatorCount <= 0) {
            int baseTicks = getSpeedAdjustedBaseTicks();
            if (this.processingTime != baseTicks || this.processingTimeTotal != baseTicks) {
                this.processingTime = baseTicks;
                this.processingTimeTotal = baseTicks;
                this.saveChanges();
            }
            return;
        }

        int requiredTicks = getProcessingDurationTicks(alternatorCount);
        long completedTime = Math.max(0L, (long) this.processingTimeTotal - this.processingTime) + 1L;
        this.processingTimeTotal = requiredTicks;
        if (completedTime >= requiredTicks) {
            finishProcessingImmediately();
            return;
        }

        this.processingTime = requiredTicks - (int) completedTime;
        if (completedTime % 20L == 0L) {
            this.saveChanges();
        }
    }

    private void finishProcessingImmediately() {
        if (!isProcessing()) {
            return;
        }

        List<GenericStack> completedOutputs = new ArrayList<>(this.processingOutputs);
        clearProcessingState();
        addOutputsToReturnInventory(completedOutputs);
        this.saveChanges();
    }

    private void clearProcessingState() {
        this.processingOutputs.clear();
        this.processingTime = 0;
        this.processingTimeTotal = 0;
        this.processingRecipeSeconds = 0;
        this.processingMultiplier = 1;
    }

    private int getAlternatorCount() {
        if (this.getWorld() == null || this.getPos() == null) {
            return 0;
        }

        int alternatorCount = 0;
        for (BlockPos checkPos : BlockPos.getAllInBox(
                this.getPos().add(-ALTERNATOR_SCAN_RADIUS, -ALTERNATOR_SCAN_RADIUS, -ALTERNATOR_SCAN_RADIUS),
                this.getPos().add(ALTERNATOR_SCAN_RADIUS, ALTERNATOR_SCAN_RADIUS, ALTERNATOR_SCAN_RADIUS))) {
            Block block = this.getWorld().getBlockState(checkPos).getBlock();
            if (ENDER_ALTERNATOR_ID.equals(block.getRegistryName())) {
                alternatorCount++;
            }
        }
        return alternatorCount;
    }

    private int getProcessingDurationTicks(int alternatorCount) {
        int baseTicks = getSpeedAdjustedBaseTicks();
        double reduction = baseTicks * (ModConfig.confEnderAlternatorEff * alternatorCount);
        return Math.max(MINIMUM_PROCESSING_TICKS, (int) (baseTicks - reduction));
    }

    private static int getRecipeSeconds(IEnderCraftingRecipe recipe) {
        int seconds = recipe.getEnderCrafterTimeSeconds();
        if (seconds <= 0) {
            seconds = ModConfig.confEnderTimeRequired;
        }
        return Math.max(1, seconds);
    }

    private int getSpeedAdjustedBaseTicks() {
        long baseTicks = 20L * Math.max(1, this.processingRecipeSeconds) * Math.max(1, this.processingMultiplier);
        double adjusted = Math.ceil(baseTicks / getSpeedMultiplier());
        return adjusted >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(MINIMUM_PROCESSING_TICKS, (int) adjusted);
    }

    private boolean shouldOutputInstantly(int alternatorCount) {
        return alternatorCount >= INSTANT_OUTPUT_ALTERNATOR_THRESHOLD;
    }
}
