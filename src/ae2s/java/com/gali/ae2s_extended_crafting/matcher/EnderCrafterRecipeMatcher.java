package com.gali.ae2s_extended_crafting.matcher;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import com.blakebr0.extendedcrafting.crafting.endercrafter.EnderCrafterRecipeManager;
import com.blakebr0.extendedcrafting.crafting.endercrafter.IEnderCraftingRecipe;
import com.blakebr0.extendedcrafting.crafting.table.TableRecipeShaped;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnderCrafterRecipeMatcher implements RecipeMatcher<IEnderCraftingRecipe> {
    private static final int LINE_SIZE = 3;
    private static final int SLOT_COUNT = LINE_SIZE * LINE_SIZE;
    private static final Container NULL_CONTAINER = new Container() {
        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return false;
        }
    };

    @Nullable
    @Override
    public IEnderCraftingRecipe match(IPatternDetails pattern, World world) {
        List<GenericStack> rawInputs = PatternStacks.sparseItemInputs(pattern);
        if (rawInputs == null) {
            return null;
        }
        List<GenericStack> sparseInputs = normalizeInputs(rawInputs);
        if (sparseInputs == null) {
            return null;
        }

        List<GenericStack> outputs = PatternStacks.itemOutputs(pattern);
        if (outputs == null || outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey)) {
            return null;
        }
        ItemStack primaryOutput = PatternStacks.toItemStack(outputs.get(0));
        if (primaryOutput.isEmpty()) {
            return null;
        }

        IEnderCraftingRecipe exact = findExactSlotRecipe(sparseInputs, primaryOutput, world);
        if (exact != null) {
            return exact;
        }

        List<AEItemKey> availableInputs = PatternStacks.expandItemInputs(sparseInputs, SLOT_COUNT);
        if (availableInputs == null) {
            return null;
        }

        for (Object object : EnderCrafterRecipeManager.getInstance().getRecipes()) {
            if (!(object instanceof IRecipe) || !(object instanceof IEnderCraftingRecipe)) {
                continue;
            }
            IRecipe recipe = (IRecipe) object;
            if (!mayProduceOutput(recipe, primaryOutput)) {
                continue;
            }
            if (matchesCondensed(recipe, availableInputs, world, primaryOutput)) {
                return (IEnderCraftingRecipe) object;
            }
        }
        return null;
    }

    @Nullable
    private static List<GenericStack> normalizeInputs(List<GenericStack> inputs) {
        if (inputs.size() > SLOT_COUNT) {
            for (int i = SLOT_COUNT; i < inputs.size(); i++) {
                if (PatternStacks.isValidItemStack(inputs.get(i))) {
                    return null;
                }
            }
            inputs = inputs.subList(0, SLOT_COUNT);
        }

        List<GenericStack> result = new ArrayList<>(inputs);
        while (result.size() < SLOT_COUNT) {
            result.add(null);
        }
        for (GenericStack input : result) {
            if (input != null && !PatternStacks.isValidItemStack(input)) {
                return null;
            }
        }
        return result;
    }

    @Nullable
    private static IEnderCraftingRecipe findExactSlotRecipe(List<GenericStack> sparseInputs, ItemStack primaryOutput,
                                                            World world) {
        if (!allInputAmountsAreSingleItems(sparseInputs)) {
            return null;
        }

        InventoryCrafting crafting = makeCraftingInventory(sparseInputs);
        for (Object object : EnderCrafterRecipeManager.getInstance().getRecipes()) {
            if (!(object instanceof IRecipe) || !(object instanceof IEnderCraftingRecipe)) {
                continue;
            }
            IRecipe recipe = (IRecipe) object;
            if (recipe.matches(crafting, world) && producesOutput(recipe, crafting, primaryOutput)) {
                return (IEnderCraftingRecipe) object;
            }
        }
        return null;
    }

    private static boolean matchesCondensed(IRecipe recipe, List<AEItemKey> availableInputs, World world,
                                            ItemStack primaryOutput) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return false;
        }

        List<AEItemKey> assignment = matchInputsToIngredients(ingredients, availableInputs);
        if (assignment == null) {
            return false;
        }

        List<GenericStack> sparseInputs = new ArrayList<>(Collections.nCopies(SLOT_COUNT, null));
        if (recipe instanceof TableRecipeShaped) {
            TableRecipeShaped shaped = (TableRecipeShaped) recipe;
            if (shaped.getWidth() > LINE_SIZE || shaped.getHeight() > LINE_SIZE) {
                return false;
            }
            for (int ingredientSlot = 0; ingredientSlot < assignment.size(); ingredientSlot++) {
                AEItemKey key = assignment.get(ingredientSlot);
                if (key == null) {
                    continue;
                }
                int x = ingredientSlot % shaped.getWidth();
                int y = ingredientSlot / shaped.getWidth();
                sparseInputs.set(y * LINE_SIZE + x, new GenericStack(key, 1));
            }
        } else {
            int sparseSlot = 0;
            for (AEItemKey key : assignment) {
                if (key != null) {
                    sparseInputs.set(sparseSlot++, new GenericStack(key, 1));
                }
            }
        }

        InventoryCrafting crafting = makeCraftingInventory(sparseInputs);
        return recipe.matches(crafting, world) && producesOutput(recipe, crafting, primaryOutput);
    }

    @Nullable
    private static List<AEItemKey> matchInputsToIngredients(NonNullList<Ingredient> ingredients,
                                                            List<AEItemKey> availableInputs) {
        int requiredInputs = 0;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.apply(ItemStack.EMPTY)) {
                requiredInputs++;
            }
        }
        if (requiredInputs != availableInputs.size()) {
            return null;
        }

        List<AEItemKey> assignment = new ArrayList<>(Collections.nCopies(ingredients.size(), null));
        boolean[] usedInputs = new boolean[availableInputs.size()];
        return assignIngredient(ingredients, availableInputs, assignment, usedInputs) ? assignment : null;
    }

    private static boolean assignIngredient(NonNullList<Ingredient> ingredients, List<AEItemKey> availableInputs,
                                            List<AEItemKey> assignment, boolean[] usedInputs) {
        int bestIngredient = -1;
        int bestCandidateCount = Integer.MAX_VALUE;
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            if (assignment.get(ingredientIndex) != null || ingredients.get(ingredientIndex).apply(ItemStack.EMPTY)) {
                continue;
            }
            int candidateCount = countCandidates(ingredients.get(ingredientIndex), availableInputs, usedInputs);
            if (candidateCount <= 0) {
                return false;
            }
            if (candidateCount < bestCandidateCount) {
                bestIngredient = ingredientIndex;
                bestCandidateCount = candidateCount;
            }
        }

        if (bestIngredient == -1) {
            return true;
        }

        Ingredient ingredient = ingredients.get(bestIngredient);
        for (int inputIndex = 0; inputIndex < availableInputs.size(); inputIndex++) {
            if (usedInputs[inputIndex] || !PatternStacks.itemMatchesIngredient(availableInputs.get(inputIndex), ingredient)) {
                continue;
            }
            usedInputs[inputIndex] = true;
            assignment.set(bestIngredient, availableInputs.get(inputIndex));
            if (assignIngredient(ingredients, availableInputs, assignment, usedInputs)) {
                return true;
            }
            assignment.set(bestIngredient, null);
            usedInputs[inputIndex] = false;
        }
        return false;
    }

    private static int countCandidates(Ingredient ingredient, List<AEItemKey> availableInputs, boolean[] usedInputs) {
        int count = 0;
        for (int inputIndex = 0; inputIndex < availableInputs.size(); inputIndex++) {
            if (!usedInputs[inputIndex] && PatternStacks.itemMatchesIngredient(availableInputs.get(inputIndex), ingredient)) {
                count++;
            }
        }
        return count;
    }

    private static InventoryCrafting makeCraftingInventory(List<GenericStack> sparseInputs) {
        InventoryCrafting crafting = new InventoryCrafting(NULL_CONTAINER, LINE_SIZE, LINE_SIZE);
        for (int i = 0; i < sparseInputs.size(); i++) {
            crafting.setInventorySlotContents(i, PatternStacks.toItemStack(sparseInputs.get(i)));
        }
        return crafting;
    }

    private static boolean allInputAmountsAreSingleItems(List<GenericStack> sparseInputs) {
        for (GenericStack stack : sparseInputs) {
            if (stack != null && stack.amount() != 1) {
                return false;
            }
        }
        return true;
    }

    private static boolean mayProduceOutput(IRecipe recipe, ItemStack primaryOutput) {
        ItemStack recipeOutput = recipe.getRecipeOutput();
        return recipeOutput.isEmpty() || ItemStack.areItemStacksEqual(recipeOutput, primaryOutput);
    }

    private static boolean producesOutput(IRecipe recipe, InventoryCrafting crafting, ItemStack primaryOutput) {
        ItemStack recipeOutput = recipe.getCraftingResult(crafting);
        return !recipeOutput.isEmpty() && ItemStack.areItemStacksEqual(recipeOutput, primaryOutput);
    }
}
