package com.gali.ae2s_extended_crafting.matcher;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import com.blakebr0.extendedcrafting.crafting.table.TableRecipeManager;
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

public class TableRecipeMatcher implements RecipeMatcher<IRecipe> {
    private static final Container NULL_CONTAINER = new Container() {
        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return false;
        }
    };

    private final int lineSize;

    public TableRecipeMatcher(int lineSize) {
        this.lineSize = lineSize;
    }

    @Nullable
    @Override
    public IRecipe match(IPatternDetails pattern, World world) {
        List<GenericStack> rawInputs = PatternStacks.sparseItemInputs(pattern);
        if (rawInputs == null) {
            return null;
        }
        List<GenericStack> sparseInputs = normalizeInputs(rawInputs);
        if (sparseInputs == null) {
            return null;
        }

        List<GenericStack> outputs = PatternStacks.itemOutputs(pattern);
        if (outputs == null) {
            return null;
        }
        if (outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey)) {
            return null;
        }
        ItemStack primaryOutput = PatternStacks.toItemStack(outputs.get(0));
        if (primaryOutput.isEmpty()) {
            return null;
        }

        IRecipe exact = findExactSlotRecipe(sparseInputs, primaryOutput, world);
        if (exact != null) {
            return exact;
        }

        List<AEItemKey> availableInputs = PatternStacks.expandItemInputs(sparseInputs, lineSize * lineSize);
        if (availableInputs == null) {
            return null;
        }

        for (Object object : TableRecipeManager.getInstance().getRecipes(this.lineSize)) {
            IRecipe recipe = (IRecipe) object;
            if (!mayProduceOutput(recipe, primaryOutput)) {
                continue;
            }
            if (matchesCondensed(recipe, availableInputs, world, primaryOutput)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    private List<GenericStack> normalizeInputs(List<GenericStack> inputs) {
        int size = this.lineSize * this.lineSize;
        if (inputs.size() > 81) {
            return null;
        }

        List<GenericStack> result = new ArrayList<>(inputs);
        if (result.size() > size) {
            for (int i = size; i < result.size(); i++) {
                if (PatternStacks.isValidItemStack(result.get(i))) {
                    return null;
                }
            }
            result = new ArrayList<>(result.subList(0, size));
        }
        while (result.size() < size) {
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
    private IRecipe findExactSlotRecipe(List<GenericStack> sparseInputs, ItemStack primaryOutput, World world) {
        if (!allInputAmountsAreSingleItems(sparseInputs)) {
            return null;
        }

        InventoryCrafting crafting = makeCraftingInventory(sparseInputs);
        for (Object object : TableRecipeManager.getInstance().getRecipes(this.lineSize)) {
            IRecipe recipe = (IRecipe) object;
            if (recipe.matches(crafting, world) && producesOutput(recipe, crafting, primaryOutput)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean matchesCondensed(IRecipe recipe, List<AEItemKey> availableInputs, World world,
                                     ItemStack primaryOutput) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return false;
        }

        List<AEItemKey> assignment = matchInputsToIngredients(ingredients, availableInputs);
        if (assignment == null) {
            return false;
        }

        List<GenericStack> sparseInputs = new ArrayList<>(Collections.nCopies(this.lineSize * this.lineSize, null));
        if (recipe instanceof TableRecipeShaped) {
            TableRecipeShaped shaped = (TableRecipeShaped) recipe;
            if (shaped.getWidth() > this.lineSize || shaped.getHeight() > this.lineSize) {
                return false;
            }
            for (int ingredientSlot = 0; ingredientSlot < assignment.size(); ingredientSlot++) {
                AEItemKey key = assignment.get(ingredientSlot);
                if (key == null) {
                    continue;
                }
                int x = ingredientSlot % shaped.getWidth();
                int y = ingredientSlot / shaped.getWidth();
                sparseInputs.set(y * this.lineSize + x, new GenericStack(key, 1));
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

    private InventoryCrafting makeCraftingInventory(List<GenericStack> sparseInputs) {
        InventoryCrafting crafting = new InventoryCrafting(NULL_CONTAINER, this.lineSize, this.lineSize);
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
