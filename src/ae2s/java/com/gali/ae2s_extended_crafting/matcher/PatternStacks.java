package com.gali.ae2s_extended_crafting.matcher;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.crafting.pattern.AEProcessingPattern;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

public final class PatternStacks {
    private PatternStacks() {
    }

    @Nullable
    public static AEProcessingPattern processing(IPatternDetails pattern) {
        IPatternDetails base = PseudoPatternDetails.unwrap(pattern);
        return base instanceof AEProcessingPattern ? (AEProcessingPattern) base : null;
    }

    public static List<GenericStack> outputs(IPatternDetails pattern) {
        return condense(pattern.getOutputs());
    }

    @Nullable
    public static List<GenericStack> itemOutputs(IPatternDetails pattern) {
        return condenseItemStacks(pattern.getOutputs());
    }

    public static List<GenericStack> sparseInputs(IPatternDetails pattern) {
        AEProcessingPattern processing = processing(pattern);
        if (processing != null) {
            return new ArrayList<>(processing.getSparseInputs());
        }

        List<GenericStack> inputs = new ArrayList<>();
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack[] possibleInputs = input.possibleInputs();
            if (possibleInputs.length != 1 || possibleInputs[0] == null) {
                return Collections.emptyList();
            }
            inputs.add(new GenericStack(possibleInputs[0].what(), possibleInputs[0].amount() * input.getMultiplier()));
        }
        return inputs;
    }

    @Nullable
    public static List<GenericStack> sparseItemInputs(IPatternDetails pattern) {
        List<GenericStack> inputs = sparseInputs(pattern);
        for (GenericStack input : inputs) {
            if (input != null && !isValidItemStack(input)) {
                return null;
            }
        }
        return inputs;
    }

    public static List<GenericStack> condensedInputs(IPatternDetails pattern) {
        return condense(sparseInputs(pattern));
    }

    @Nullable
    public static List<GenericStack> condensedItemInputs(IPatternDetails pattern) {
        return condenseItemStacks(sparseInputs(pattern));
    }

    public static List<GenericStack> condense(List<GenericStack> stacks) {
        Map<AEKey, Long> totals = new LinkedHashMap<>();
        for (GenericStack stack : stacks) {
            if (!isValidItemStack(stack)) {
                continue;
            }
            totals.merge(stack.what(), stack.amount(), Long::sum);
        }

        List<GenericStack> result = new ArrayList<>();
        for (Map.Entry<AEKey, Long> entry : totals.entrySet()) {
            if (entry.getValue() > 0) {
                result.add(new GenericStack(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    @Nullable
    public static List<GenericStack> condenseItemStacks(List<GenericStack> stacks) {
        Map<AEKey, Long> totals = new LinkedHashMap<>();
        for (GenericStack stack : stacks) {
            if (stack == null) {
                continue;
            }
            if (!isValidItemStack(stack)) {
                return null;
            }
            totals.merge(stack.what(), stack.amount(), Long::sum);
        }

        List<GenericStack> result = new ArrayList<>();
        for (Map.Entry<AEKey, Long> entry : totals.entrySet()) {
            if (entry.getValue() > 0) {
                result.add(new GenericStack(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    @Nullable
    public static List<AEItemKey> expandItemInputs(List<GenericStack> inputs, int maxItems) {
        List<AEItemKey> expanded = new ArrayList<>();
        for (GenericStack input : inputs) {
            if (input == null || input.amount() <= 0) {
                continue;
            }
            if (!(input.what() instanceof AEItemKey) || input.amount() > Integer.MAX_VALUE) {
                return null;
            }
            for (long i = 0; i < input.amount(); i++) {
                expanded.add((AEItemKey) input.what());
                if (expanded.size() > maxItems) {
                    return null;
                }
            }
        }
        return expanded;
    }

    public static boolean outputMatches(List<GenericStack> patternOutputs, ItemStack recipeOutput) {
        if (recipeOutput.isEmpty()) {
            return patternOutputs.isEmpty();
        }
        if (patternOutputs.size() != 1) {
            return false;
        }
        GenericStack output = patternOutputs.get(0);
        return output.what() instanceof AEItemKey
                && output.amount() == recipeOutput.getCount()
                && ((AEItemKey) output.what()).matches(recipeOutput);
    }

    public static boolean itemMatchesIngredient(AEItemKey key, Ingredient ingredient) {
        return ingredient.apply(key.toStack());
    }

    public static ItemStack toItemStack(@Nullable GenericStack stack) {
        if (stack == null || stack.amount() <= 0 || stack.amount() > Integer.MAX_VALUE
                || !(stack.what() instanceof AEItemKey)) {
            return ItemStack.EMPTY;
        }
        return ((AEItemKey) stack.what()).toStack((int) stack.amount());
    }

    public static boolean isValidItemStack(@Nullable GenericStack stack) {
        return stack != null
                && stack.amount() > 0
                && stack.amount() <= Integer.MAX_VALUE
                && stack.what() instanceof AEItemKey;
    }

    public static int getInputMultiplier(KeyCounter[] inputHolder, IPatternDetails pattern) {
        IPatternDetails.IInput[] inputs = pattern.getInputs();
        long multiplier = Long.MAX_VALUE;
        for (int i = 0; i < inputs.length && i < inputHolder.length; i++) {
            long expected = inputs[i].getMultiplier();
            if (expected <= 0) {
                continue;
            }

            long actual = 0;
            for (Object2LongMap.Entry<AEKey> entry : inputHolder[i]) {
                actual += entry.getLongValue();
            }
            multiplier = Math.min(multiplier, actual / expected);
        }
        if (multiplier == Long.MAX_VALUE || multiplier <= 0) {
            return 1;
        }
        return multiplier > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) multiplier;
    }
}
