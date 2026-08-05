package com.gali.ae2s_extended_crafting.matcher;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import com.blakebr0.extendedcrafting.crafting.CombinationRecipe;
import com.blakebr0.extendedcrafting.crafting.CombinationRecipeManager;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CombinationRecipeMatcher implements RecipeMatcher<CombinationRecipe> {
    @Nullable
    @Override
    public CombinationRecipe match(IPatternDetails pattern, World world) {
        List<GenericStack> outputs = PatternStacks.itemOutputs(pattern);
        List<GenericStack> inputs = PatternStacks.condensedItemInputs(pattern);
        if (outputs == null || inputs == null) {
            return null;
        }
        if (inputs.isEmpty() || outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey)) {
            return null;
        }

        for (CombinationRecipe recipe : CombinationRecipeManager.getInstance().getRecipes()) {
            if (!PatternStacks.outputMatches(outputs, recipe.getOutput())) {
                continue;
            }
            if (matchesInputs(inputs, recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean matchesInputs(List<GenericStack> condensedInputs, CombinationRecipe recipe) {
        List<AEItemKey> expanded = PatternStacks.expandItemInputs(condensedInputs, 82);
        if (expanded == null || expanded.size() != recipe.getPedestalItems().size() + 1) {
            return false;
        }

        for (int centerIndex = 0; centerIndex < expanded.size(); centerIndex++) {
            ItemStack center = expanded.get(centerIndex).toStack();
            if (!matchesCoreInput(recipe.getInput(), center)) {
                continue;
            }

            List<AEItemKey> remaining = new ArrayList<>(expanded);
            remaining.remove(centerIndex);
            if (matchPedestals(recipe.getPedestalItems(), remaining)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCoreInput(ItemStack template, ItemStack input) {
        return !template.isEmpty() && template.isItemEqual(input) && compareTags(template, input);
    }

    private static boolean matchPedestals(List<?> pedestalIngredients, List<AEItemKey> inputs) {
        if (pedestalIngredients.size() != inputs.size()) {
            return false;
        }
        boolean[] used = new boolean[inputs.size()];
        return assignPedestal(pedestalIngredients, inputs, used);
    }

    private static boolean assignPedestal(List<?> pedestalIngredients, List<AEItemKey> inputs, boolean[] used) {
        int bestIngredient = -1;
        int bestCandidateCount = Integer.MAX_VALUE;
        for (int ingredientIndex = 0; ingredientIndex < pedestalIngredients.size(); ingredientIndex++) {
            int candidateCount = countPedestalCandidates(pedestalIngredients.get(ingredientIndex), inputs, used);
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

        Object ingredient = pedestalIngredients.get(bestIngredient);
        List<Object> nextIngredients = new ArrayList<>(pedestalIngredients);
        nextIngredients.remove(bestIngredient);
        for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
            if (used[inputIndex] || !matchesPedestal(ingredient, inputs.get(inputIndex))) {
                continue;
            }
            used[inputIndex] = true;
            if (assignPedestal(nextIngredients, inputs, used)) {
                return true;
            }
            used[inputIndex] = false;
        }
        return false;
    }

    private static int countPedestalCandidates(Object ingredient, List<AEItemKey> inputs, boolean[] used) {
        int count = 0;
        for (int i = 0; i < inputs.size(); i++) {
            if (!used[i] && matchesPedestal(ingredient, inputs.get(i))) {
                count++;
            }
        }
        return count;
    }

    private static boolean matchesPedestal(Object ingredient, AEItemKey input) {
        ItemStack inputStack = input.toStack();
        if (ingredient instanceof ItemStack) {
            ItemStack template = (ItemStack) ingredient;
            return OreDictionary.itemMatches(template, inputStack, false)
                    && (!template.hasTagCompound() || compareTags(template, inputStack));
        }
        if (ingredient instanceof Item) {
            return OreDictionary.itemMatches(new ItemStack((Item) ingredient), inputStack, false);
        }
        if (ingredient instanceof Block) {
            return OreDictionary.itemMatches(new ItemStack((Block) ingredient), inputStack, false);
        }
        if (ingredient instanceof String) {
            return matchesAnyOre((String) ingredient, inputStack);
        }
        if (ingredient instanceof List<?>) {
            for (Object option : (List<?>) ingredient) {
                if (option instanceof ItemStack && OreDictionary.itemMatches((ItemStack) option, inputStack, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesAnyOre(String oreName, ItemStack input) {
        for (ItemStack stack : OreDictionary.getOres(oreName)) {
            if (!stack.isEmpty() && OreDictionary.itemMatches(stack, input, false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean compareTags(ItemStack template, ItemStack input) {
        if (!template.hasTagCompound()) {
            return true;
        }
        if (!input.hasTagCompound()) {
            return false;
        }

        NBTTagCompound templateTag = template.getTagCompound();
        NBTTagCompound inputTag = input.getTagCompound();
        if (templateTag == null || inputTag == null) {
            return templateTag == null && inputTag == null;
        }

        for (String key : templateTag.getKeySet()) {
            if (!inputTag.hasKey(key) || !NBTUtil.areNBTEquals(templateTag.getTag(key), inputTag.getTag(key), true)) {
                return false;
            }
        }
        return true;
    }
}
