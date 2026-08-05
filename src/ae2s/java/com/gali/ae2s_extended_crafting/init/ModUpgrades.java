package com.gali.ae2s_extended_crafting.init;

import ae2.api.upgrades.Upgrades;
import ae2.core.AEConfig;
import ae2.core.definitions.AEItems;
import net.minecraft.item.Item;

public final class ModUpgrades {
    private static final int PARALLEL_CARD_LIMIT = 3;
    private static final int SPEED_CARD_LIMIT = 5;
    private static boolean registered;

    private ModUpgrades() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        int patternExpansionLimit = AEConfig.instance().getPatternProviderExpansionCardLimit();

        registerStation(ModBlocks.TABLE_BASIC_PATTERN_PROVIDER.item(), patternExpansionLimit, "table_basic");
        registerStation(ModBlocks.TABLE_ADVANCED_PATTERN_PROVIDER.item(), patternExpansionLimit, "table_advanced");
        registerStation(ModBlocks.TABLE_ELITE_PATTERN_PROVIDER.item(), patternExpansionLimit, "table_elite");
        registerStation(ModBlocks.TABLE_ULTIMATE_PATTERN_PROVIDER.item(), patternExpansionLimit, "table_ultimate");
        registerStation(ModBlocks.CRAFTER_CORE_PATTERN_PROVIDER.item(), patternExpansionLimit, "crafter_core");
        registerStation(ModBlocks.ENDER_CRAFTER_PATTERN_PROVIDER.item(), patternExpansionLimit, "ender_crafter");

        registered = true;
    }

    private static void registerStation(Item station, int patternExpansionLimit, String slotId) {
        registerPatternProviderCards(station, patternExpansionLimit);
        Upgrades.add(AEItems.PARALLEL_CARD.item(), station, PARALLEL_CARD_LIMIT);
        Upgrades.add(AEItems.SPEED_CARD.item(), station, SPEED_CARD_LIMIT);
        Upgrades.addUpgradeSlots(station, ModBlocks.id(slotId + "_parallel_slots"), PARALLEL_CARD_LIMIT);
        Upgrades.addUpgradeSlots(station, ModBlocks.id(slotId + "_speed_slots"), SPEED_CARD_LIMIT);
    }

    private static void registerPatternProviderCards(Item station, int patternExpansionLimit) {
        if (patternExpansionLimit > 0) {
            Upgrades.add(AEItems.PATTERN_EXPANSION_CARD.item(), station, patternExpansionLimit);
        }
    }
}
