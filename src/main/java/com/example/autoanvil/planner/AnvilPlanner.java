package com.example.autoanvil.planner;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prior-work-aware book planner.
 *
 * It models the vanilla 0,1,3,7,15... repair-cost penalty and the vanilla
 * enchantment anvil weights. The server remains authoritative: automation
 * only takes an output when the actual AnvilScreenHandler reports a valid,
 * non-Too-Expensive result.
 */
public final class AnvilPlanner {
    private AnvilPlanner() {}

    public record Candidate(ItemStack stack, int inventorySlot, int priorWork, int enchantCount) {}

    public static int priorPenalty(int priorWork) {
        if (priorWork <= 0) return 0;
        if (priorWork >= 31) return Integer.MAX_VALUE / 4;
        return (1 << priorWork) - 1;
    }

    public static int enchantmentTransferCost(ItemStack sacrifice) {
        int total = 0;
        for (RegistryEntry<Enchantment> entry : EnchantmentHelper.getEnchantments(sacrifice).getEnchantments()) {
            int level = EnchantmentHelper.getLevel(entry, sacrifice);
            int weight = Math.max(1, entry.value().getAnvilCost());
            // Enchanted books pay half the normal enchantment cost, rounded down,
            // with a minimum of one per transferred enchantment level.
            total += Math.max(1, weight / 2) * level;
        }
        return total;
    }

    public static int estimateCombineCost(ItemStack target, ItemStack sacrifice) {
        int base = target.getRepairCost() + sacrifice.getRepairCost();
        return base + enchantmentTransferCost(sacrifice);
    }

    /**
     * Stable heuristic: low prior-work books first, then low transfer cost,
     * while keeping equal/similar books together. This is the same core idea
     * used by balanced anvil calculators and is deliberately bounded for in-game use.
     */
    public static List<Candidate> orderBooks(List<Candidate> books) {
        List<Candidate> copy = new ArrayList<>(books);
        copy.sort(Comparator
            .comparingInt(Candidate::priorWork)
            .thenComparingInt(Candidate::enchantCount)
            .thenComparingInt(c -> enchantmentTransferCost(c.stack()))
            .thenComparingInt(Candidate::inventorySlot));
        return copy;
    }

    /**
     * Produces a plan by repeatedly combining the cheapest pair according to
     * estimated vanilla cost plus the future prior-work penalty. The resulting
     * order is revalidated against the live server at every step.
     */
    public static AnvilPlan plan(ItemStack base, List<ItemStack> sacrifices) {
        if (base == null || base.isEmpty() || sacrifices.isEmpty()) {
            return new AnvilPlan(List.of(), 0);
        }

        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(base.copy()));
        for (ItemStack stack : sacrifices) {
            if (stack != null && !stack.isEmpty()) nodes.add(new Node(stack.copy()));
        }

        List<AnvilPlan.Step> steps = new ArrayList<>();
        int total = 0;
        while (nodes.size() > 1) {
            Pair best = null;
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    Node a = nodes.get(i), b = nodes.get(j);
                    int costAB = score(a.stack, b.stack);
                    int costBA = score(b.stack, a.stack);
                    int cost = Math.min(costAB, costBA);
                    if (best == null || cost < best.score) best = new Pair(i, j, cost, costAB <= costBA);
                }
            }
            if (best == null) break;
            Node left = nodes.get(best.left);
            Node right = nodes.get(best.right);
            if (!best.leftFirst) {
                Node tmp = left; left = right; right = tmp;
            }
            int cost = score(left.stack, right.stack);
            steps.add(new AnvilPlan.Step(left.stack.copy(), right.stack.copy(), cost,
                left.stack.getRepairCost(), right.stack.getRepairCost()));
            total += Math.max(0, cost);
            Node merged = new Node(left.stack.copy());
            merged.stack.setRepairCost(AnvilScreenHandler.getNextCost(Math.max(left.stack.getRepairCost(), right.stack.getRepairCost())));
            nodes.remove(Math.max(best.left, best.right));
            nodes.remove(Math.min(best.left, best.right));
            nodes.add(merged);
        }
        return new AnvilPlan(steps, total);
    }

    private static int score(ItemStack target, ItemStack sacrifice) {
        long value = (long) target.getRepairCost() + sacrifice.getRepairCost()
            + enchantmentTransferCost(sacrifice) + priorPenalty(target.getRepairCost());
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static final class Node {
        final ItemStack stack;
        Node(ItemStack stack) { this.stack = stack; }
    }

    private record Pair(int left, int right, int score, boolean leftFirst) {}
}
