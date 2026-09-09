package com.example.autoanvil;

import com.example.autoanvil.planner.AnvilPlan;
import com.example.autoanvil.planner.AnvilPlanner;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AutoEnchantController {
    private boolean enabled;
    private boolean waitingForXp;
    private boolean running;
    private final Map<RegistryEntry<Enchantment>, Integer> requested = new LinkedHashMap<>();
    private final Deque<Integer> itemQueue = new ArrayDeque<>();
    private int selectedInventorySlot = -1;
    private int lastActionTick = -100;
    private int tickCounter;
    private String status = "OFF";
    private AnvilPlan previewPlan = new AnvilPlan(List.of(), 0);

    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean value) {
        enabled = value;
        if (!value) { running = false; waitingForXp = false; status = "OFF"; }
        else status = "READY";
    }
    public boolean isRunning() { return running; }
    public boolean isWaitingForXp() { return waitingForXp; }
    public String status() { return status; }
    public Map<RegistryEntry<Enchantment>, Integer> requested() { return requested; }
    public void clearRequested() { requested.clear(); previewPlan = new AnvilPlan(List.of(), 0); }
    public void addRequested(RegistryEntry<Enchantment> enchantment, int level) {
        requested.put(enchantment, Math.max(1, Math.min(level, enchantment.value().getMaxLevel())));
    }
    public void removeRequested(RegistryEntry<Enchantment> enchantment) { requested.remove(enchantment); }
    public int selectedInventorySlot() { return selectedInventorySlot; }
    public void setSelectedInventorySlot(int slot) { selectedInventorySlot = slot; }
    public AnvilPlan previewPlan() { return previewPlan; }

    public void rebuildQueue(MinecraftClient client) {
        if (client.player == null) return;
        itemQueue.clear();
        itemQueue.addAll(InventoryScanner.supportedInventorySlots(client.player.getInventory()));
        status = itemQueue.isEmpty() ? "NO TOOL/ARMOR" : "QUEUE " + itemQueue.size();
    }

    public void start(MinecraftClient client) {
        if (client.player == null) return;
        rebuildQueue(client);
        running = !itemQueue.isEmpty();
        waitingForXp = false;
        status = running ? "RUNNING" : "NO ITEMS";
    }

    public void stop() {
        running = false;
        waitingForXp = false;
        status = enabled ? "READY" : "OFF";
    }

    private void tick(MinecraftClient client) {
        tickCounter++;
        if (!enabled || !(client.currentScreen instanceof AnvilScreen screen) || client.player == null) return;
        if (!running) return;
        if (tickCounter - lastActionTick < 5) return;

        AnvilScreenHandler handler = screen.getScreenHandler();
        ItemStack output = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
        int cost = handler.getLevelCost();

        if (!output.isEmpty() && cost > 0) {
            if (cost >= 40 && !client.player.getAbilities().creativeMode) {
                status = "TOO EXPENSIVE - REPLAN";
                stop();
                return;
            }
            if (client.player.experienceLevel < cost && !client.player.getAbilities().creativeMode) {
                waitingForXp = true;
                status = "WAIT XP " + cost;
                return;
            }
            waitingForXp = false;
            status = "APPLY " + cost + " XP";
            client.interactionManager.clickSlot(handler.syncId, AnvilScreenHandler.OUTPUT_ID, 0,
                SlotActionType.PICKUP, client.player);
            lastActionTick = tickCounter;
            return;
        }

        if (handler.getSlot(AnvilScreenHandler.INPUT_1_ID).getStack().isEmpty()) {
            if (itemQueue.isEmpty()) {
                rebuildQueue(client);
                if (itemQueue.isEmpty()) { stop(); return; }
            }
            int invIndex = itemQueue.peek();
            if (client.player.getInventory().getStack(invIndex).isEmpty()) {
                itemQueue.remove();
                return;
            }
            java.util.OptionalInt screenSlot = handler.getSlotIndex(client.player.getInventory(), invIndex);
            if (screenSlot.isPresent()) {
                client.interactionManager.clickSlot(handler.syncId, screenSlot.getAsInt(), 0,
                    SlotActionType.QUICK_MOVE, client.player);
                selectedInventorySlot = invIndex;
                lastActionTick = tickCounter;
                return;
            }
        }

        if (!handler.getSlot(AnvilScreenHandler.INPUT_1_ID).getStack().isEmpty()
            && handler.getSlot(AnvilScreenHandler.INPUT_2_ID).getStack().isEmpty()) {
            List<Integer> books = InventoryScanner.matchingBooks(client.player.getInventory(), requested);
            if (!books.isEmpty()) {
                int invIndex = books.get(0);
                java.util.OptionalInt screenSlot = handler.getSlotIndex(client.player.getInventory(), invIndex);
                if (screenSlot.isPresent()) {
                    client.interactionManager.clickSlot(handler.syncId, screenSlot.getAsInt(), 0,
                        SlotActionType.QUICK_MOVE, client.player);
                    lastActionTick = tickCounter;
                    status = "BOOK " + (invIndex + 1);
                    return;
                }
            } else {
                status = "NO ENCHANTED BOOK";
                stop();
            }
        }
    }

    public void refreshPreview(MinecraftClient client) {
        if (client.player == null) return;
        ItemStack base = client.player.getInventory().getStack(Math.max(0, selectedInventorySlot));
        if (base.isEmpty()) return;
        List<ItemStack> books = new ArrayList<>();
        for (int slot : InventoryScanner.matchingBooks(client.player.getInventory(), requested)) {
            books.add(client.player.getInventory().getStack(slot));
        }
        previewPlan = AnvilPlanner.plan(base, books);
    }
}
