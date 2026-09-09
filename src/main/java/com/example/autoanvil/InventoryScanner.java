package com.example.autoanvil;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

import java.util.ArrayList;
import java.util.List;

public final class InventoryScanner {
    private InventoryScanner() {}

    public static boolean isSupportedToolOrArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof ArmorItem
            || stack.isIn(ItemTags.PICKAXES)
            || stack.isIn(ItemTags.AXES)
            || stack.isIn(ItemTags.SHOVELS)
            || stack.isIn(ItemTags.HOES);
    }

    public static List<Integer> supportedInventorySlots(net.minecraft.entity.player.PlayerInventory inventory) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            if (isSupportedToolOrArmor(inventory.getStack(i))) result.add(i);
        }
        return result;
    }

    public static List<Integer> matchingBooks(net.minecraft.entity.player.PlayerInventory inventory,
                                              java.util.Map<net.minecraft.registry.entry.RegistryEntry<net.minecraft.enchantment.Enchantment>, Integer> requested) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty() || !EnchantmentHelper.hasEnchantments(stack)) continue;
            var enchants = EnchantmentHelper.getEnchantments(stack);
            boolean useful = requested.isEmpty();
            for (var entry : requested.entrySet()) {
                if (enchants.getLevel(entry.getKey()) >= entry.getValue()) {
                    useful = true;
                    break;
                }
            }
            if (useful) result.add(i);
        }
        return result;
    }
}
