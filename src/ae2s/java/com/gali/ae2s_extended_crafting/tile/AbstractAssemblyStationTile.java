package com.gali.ae2s_extended_crafting.tile;

import ae2.api.config.Actionable;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.util.IConfigManager;
import ae2.block.crafting.PushDirection;
import ae2.core.definitions.AEItems;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import ae2.items.tools.MemoryCardItem;
import ae2.tile.ServerTickingTile;
import ae2.tile.grid.AENetworkedTile;
import ae2.util.SettingsFrom;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.filter.IAEItemFilter;
import com.gali.ae2s_extended_crafting.block.AssemblyStationBlock;
import com.gali.ae2s_extended_crafting.matcher.PatternStacks;
import com.gali.ae2s_extended_crafting.matcher.RecipeMatcher;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public abstract class AbstractAssemblyStationTile<M> extends AENetworkedTile
        implements ServerTickingTile, PatternProviderLogicHost {
    private static final String MEMORY_CARD_PUSH_DIRECTION = "pushDirection";
    private static final PushDirection[] PUSH_DIRECTIONS = PushDirection.values();

    private final StationPatternProviderLogic logic;

    protected AbstractAssemblyStationTile() {
        this.logic = new StationPatternProviderLogic(this.getMainNode(), this);
    }

    protected abstract RecipeMatcher<M> getRecipeMatcher();

    protected abstract Block getStationBlock();

    protected boolean canAcceptMatchedRecipe(M match) {
        return true;
    }

    protected boolean enqueueMatchedOutputs(IPatternDetails pattern, M match, int multiplier) {
        return addOutputsToReturnInventory(createMultipliedOutputs(pattern.getOutputs(), multiplier));
    }

    protected List<GenericStack> createMultipliedOutputs(List<GenericStack> patternOutputs, int multiplier) {
        List<GenericStack> outputs = new ArrayList<>();
        for (GenericStack output : patternOutputs) {
            if (output == null || output.amount() <= 0) {
                continue;
            }
            long amount = saturatedMultiply(output.amount(), Math.max(1, multiplier));
            outputs.add(new GenericStack(output.what(), amount));
        }
        return outputs;
    }

    protected static long saturatedMultiply(long amount, int multiplier) {
        if (multiplier <= 1) {
            return amount;
        }
        return amount > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : amount * multiplier;
    }

    protected boolean addOutputsToReturnInventory(List<GenericStack> outputs) {
        if (outputs.isEmpty()) {
            return false;
        }
        if (!canFitOutputsInReturnInventory(outputs)) {
            return false;
        }
        for (GenericStack output : outputs) {
            insertOutputIntoReturnInventory(output, Actionable.MODULATE);
        }
        this.saveChanges();
        this.alertTickManager();
        return true;
    }

    private boolean canFitOutputsInReturnInventory(List<GenericStack> outputs) {
        List<GenericStack> simulatedSlots = new ArrayList<>();
        for (int slot = 0; slot < this.logic.getReturnInv().size(); slot++) {
            simulatedSlots.add(this.logic.getReturnInv().getStack(slot));
        }

        for (GenericStack output : outputs) {
            long remaining = output.amount();
            for (int slot = 0; slot < simulatedSlots.size() && remaining > 0; slot++) {
                GenericStack current = simulatedSlots.get(slot);
                if (current != null && !current.what().equals(output.what())) {
                    continue;
                }

                long currentAmount = current == null ? 0 : current.amount();
                long maxAmount = this.logic.getReturnInv().getMaxAmount(output.what());
                if (currentAmount >= maxAmount) {
                    continue;
                }

                long inserted = Math.min(remaining, maxAmount - currentAmount);
                remaining -= inserted;
                simulatedSlots.set(slot, new GenericStack(output.what(), currentAmount + inserted));
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private long insertOutputIntoReturnInventory(GenericStack output, Actionable mode) {
        long inserted = 0;
        for (int slot = 0; slot < this.logic.getReturnInv().size() && inserted < output.amount(); slot++) {
            inserted += this.logic.getReturnInv().insert(
                    slot,
                    output.what(),
                    output.amount() - inserted,
                    mode
            );
        }
        return inserted;
    }

    protected boolean hasPendingOutputs() {
        return !this.logic.getReturnInv().isEmpty();
    }

    protected boolean isBusyForPush() {
        return hasPendingOutputs();
    }

    protected boolean supportsMultiplier(IPatternDetails pattern) {
        return true;
    }

    protected int getMaxMultiplier(IPatternDetails pattern, int maxMultiplier) {
        int limitedMaxMultiplier = Math.min(maxMultiplier, getParallelLimit());
        if (isBusyForPush() || limitedMaxMultiplier <= 0) {
            return 0;
        }
        if (canFitOutputsInReturnInventory(createMultipliedOutputs(pattern.getOutputs(), limitedMaxMultiplier))) {
            return limitedMaxMultiplier;
        }

        int lower = 0;
        int upper = limitedMaxMultiplier;
        while (lower < upper) {
            int middle = lower + (upper - lower + 1) / 2;
            if (canFitOutputsInReturnInventory(createMultipliedOutputs(pattern.getOutputs(), middle))) {
                lower = middle;
            } else {
                upper = middle - 1;
            }
        }
        return lower;
    }

    @Override
    public int getInstalledUpgrades(Item upgradeCard) {
        return this.logic.getUpgrades().getInstalledUpgrades(upgradeCard);
    }

    protected int getParallelLimit() {
        int parallelCards = getInstalledUpgrades(AEItems.PARALLEL_CARD.item());
        if (parallelCards == 1) {
            return 4;
        }
        if (parallelCards == 2) {
            return 16;
        }
        if (parallelCards >= 3) {
            return 64;
        }
        return 1;
    }

    protected double getSpeedMultiplier() {
        int speedCards = getInstalledUpgrades(AEItems.SPEED_CARD.item());
        if (speedCards == 1) {
            return 1.3;
        }
        if (speedCards == 2) {
            return 1.7;
        }
        if (speedCards == 3) {
            return 2.0;
        }
        if (speedCards == 4) {
            return 2.5;
        }
        if (speedCards >= 5) {
            return 5.0;
        }
        return 1.0;
    }

    protected boolean pushMatchedPattern(IPatternDetails pattern, M match, KeyCounter[] inputHolder, int multiplier) {
        return enqueueMatchedOutputs(pattern, match, multiplier);
    }

    protected boolean isPatternSupported(IPatternDetails pattern) {
        if (this.getWorld() == null) {
            return false;
        }
        IPatternDetails base = PseudoPatternDetails.unwrap(pattern);
        if (PatternStacks.processing(base) == null) {
            return false;
        }
        M match = this.getRecipeMatcher().match(base, this.getWorld());
        return match != null && this.canAcceptMatchedRecipe(match);
    }

    protected boolean isPatternItemValid(ItemStack stack) {
        if (stack.isEmpty() || this.getWorld() == null) {
            return false;
        }
        try {
            IPatternDetails details = PatternDetailsHelper.decodePattern(stack, this.getWorld());
            return details != null && isPatternSupported(details);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void alertTickManager() {
        this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    @Override
    public void serverTick() {
        IGrid grid = this.getMainNode().getGrid();
        if (grid != null && !this.logic.getReturnInv().isEmpty()) {
            this.logic.getReturnInv().injectIntoNetwork(
                    grid.getStorageService().getInventory(),
                    ae2.api.networking.security.IActionSource.ofMachine(this),
                    ignored -> {
                    }
            );
        }
    }

    @Override
    public PatternProviderLogic getLogic() {
        return this.logic;
    }

    @Override
    public void onReady() {
        super.onReady();
        this.logic.updatePatterns();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.logic.onMainNodeStateChanged();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.logic.writeToNBT(data);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.logic.readFromNBT(data);
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        this.logic.invalidateTargetCaches();
        this.onGridConnectableSidesChanged();
    }

    public void onPushDirectionChanged() {
        this.logic.invalidateTargetCaches();
        this.onGridConnectableSidesChanged();
    }

    @Override
    public void exportSettings(SettingsFrom mode, NBTTagCompound output) {
        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }
        MemoryCardItem.exportGenericSettings(this, output);
        this.logic.exportSettings(output);
        output.setInteger(MEMORY_CARD_PUSH_DIRECTION, this.getPushDirection().ordinal());
    }

    @Override
    public void importSettings(SettingsFrom mode, NBTTagCompound input, @Nullable EntityPlayer player) {
        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }
        MemoryCardItem.importGenericSettings(this, input, player);
        this.logic.importSettings(input, player);
        if (input.hasKey(MEMORY_CARD_PUSH_DIRECTION, 3)) {
            int ordinal = input.getInteger(MEMORY_CARD_PUSH_DIRECTION);
            IBlockState state = this.getBlockState();
            if (ordinal >= 0 && ordinal < PUSH_DIRECTIONS.length
                    && state != null && state.getBlock() instanceof AssemblyStationBlock) {
                this.getWorld().setBlockState(
                        this.getPos(),
                        state.withProperty(AssemblyStationBlock.PUSH_DIRECTION, PUSH_DIRECTIONS[ordinal]),
                        3
                );
                this.onGridConnectableSidesChanged();
            }
        }
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(this.getStationBlock());
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        EnumFacing pushDirection = this.getPushDirection().getDirection();
        return pushDirection == null ? EnumSet.allOf(EnumFacing.class) : EnumSet.of(pushDirection);
    }

    @Override
    public EnumSet<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        EnumFacing pushDirection = this.getPushDirection().getDirection();
        return pushDirection == null ? EnumSet.allOf(EnumFacing.class) : EnumSet.complementOf(EnumSet.of(pushDirection));
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        this.logic.clearContent();
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(this.getStationBlock());
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(this.getStationBlock()));
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.logic.getConfigManager();
    }

    private PushDirection getPushDirection() {
        IBlockState state = this.getBlockState();
        if (state == null || !(state.getBlock() instanceof AssemblyStationBlock)) {
            return PushDirection.ALL;
        }
        return (PushDirection) state.getValue(AssemblyStationBlock.PUSH_DIRECTION);
    }

    private final class StationPatternProviderLogic extends PatternProviderLogic {
        private StationPatternProviderLogic(IManagedGridNode mainNode, AbstractAssemblyStationTile<M> host) {
            super(mainNode, host);
            AppEngInternalInventory patternInventory = this.getPatternInv();
            patternInventory.setFilter(new IAEItemFilter() {
                @Override
                public boolean allowInsert(ae2.api.inventories.InternalInventory inv, int slot, ItemStack stack) {
                    return AbstractAssemblyStationTile.this.isPatternItemValid(stack)
                            && !hasSamePattern(inv, slot, stack);
                }
            });
        }

        @Override
        public List<IPatternDetails> getAvailablePatterns() {
            List<IPatternDetails> result = new ArrayList<>();
            for (IPatternDetails pattern : super.getAvailablePatterns()) {
                if (AbstractAssemblyStationTile.this.isPatternSupported(pattern)) {
                    result.add(pattern);
                }
            }
            return result;
        }

        @Override
        public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
            IPatternDetails base = PseudoPatternDetails.unwrap(patternDetails);
            if (!isInstalledAndActive(base)) {
                return false;
            }
            M match = AbstractAssemblyStationTile.this.getRecipeMatcher().match(base,
                    AbstractAssemblyStationTile.this.getWorld());
            if (match == null || !AbstractAssemblyStationTile.this.canAcceptMatchedRecipe(match)
                    || AbstractAssemblyStationTile.this.isBusyForPush()) {
                return false;
            }
            return AbstractAssemblyStationTile.this.pushMatchedPattern(base, match, inputHolder, multiplier);
        }

        @Override
        public boolean canMergePatternPush(IPatternDetails patternDetails) {
            IPatternDetails base = PseudoPatternDetails.unwrap(patternDetails);
            return isInstalledAndActive(base)
                    && AbstractAssemblyStationTile.this.isPatternSupported(base)
                    && AbstractAssemblyStationTile.this.supportsMultiplier(base);
        }

        @Override
        public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
            IPatternDetails base = PseudoPatternDetails.unwrap(patternDetails);
            if (!isInstalledAndActive(base) || !AbstractAssemblyStationTile.this.isPatternSupported(base)) {
                return 0;
            }
            return AbstractAssemblyStationTile.this.getMaxMultiplier(base, maxMultiplier);
        }

        @Override
        public boolean isBusy() {
            return AbstractAssemblyStationTile.this.isBusyForPush();
        }

        private boolean isInstalledAndActive(IPatternDetails pattern) {
            return AbstractAssemblyStationTile.this.getMainNode().isActive()
                    && this.containsPattern(pattern.getDefinition());
        }

        private boolean hasSamePattern(ae2.api.inventories.InternalInventory inv, int slot, ItemStack stack) {
            AEItemKey pattern = AEItemKey.of(stack);
            if (pattern == null) {
                return false;
            }
            for (int i = 0; i < inv.size(); i++) {
                AEItemKey otherPattern;
                if (i == slot || !pattern.equals(otherPattern = AEItemKey.of(inv.getStackInSlot(i)))) {
                    continue;
                }
                return true;
            }
            return false;
        }
    }
}
