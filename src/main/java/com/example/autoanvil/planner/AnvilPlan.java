package com.example.autoanvil.planner;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable plan for a sequence of vanilla anvil operations. */
public final class AnvilPlan {
    public record Step(ItemStack left, ItemStack right, int estimatedCost, int leftPriorWork, int rightPriorWork) {}

    private final List<Step> steps;
    private final int estimatedTotalCost;

    public AnvilPlan(List<Step> steps, int estimatedTotalCost) {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.estimatedTotalCost = estimatedTotalCost;
    }

    public List<Step> steps() { return steps; }
    public int estimatedTotalCost() { return estimatedTotalCost; }
}
