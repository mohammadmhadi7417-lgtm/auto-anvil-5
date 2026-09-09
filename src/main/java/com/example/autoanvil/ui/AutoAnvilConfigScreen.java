package com.example.autoanvil.ui;

import com.example.autoanvil.AutoAnvilClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class AutoAnvilConfigScreen extends Screen {
    private final Screen parent;
    private final List<RegistryEntry<Enchantment>> enchantments = new ArrayList<>();
    private int page;

    public AutoAnvilConfigScreen(Screen parent) {
        super(Text.literal("Auto Enchant"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        enchantments.clear();
        if (client != null && client.world != null) {
            client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).streamEntries()
                .filter(e -> e.value().getMaxLevel() > 0)
                .forEach(enchantments::add);
        }
        enchantments.sort(java.util.Comparator.comparing(e -> e.value().description().getString()));
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            AutoAnvilClient.CONTROLLER.clearRequested();
            rebuild();
        }).dimensions(width / 2 - 154, height - 28, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(width / 2 + 84, height - 28, 70, 20).build());
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        int perPage = 12;
        int maxPage = Math.max(0, (enchantments.size() - 1) / perPage);
        page = Math.max(0, Math.min(page, maxPage));
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < enchantments.size(); i++) {
            RegistryEntry<Enchantment> entry = enchantments.get(start + i);
            int level = AutoAnvilClient.CONTROLLER.requested().getOrDefault(entry, 0);
            String label = entry.value().description().getString() + (level > 0 ? " " + level : " -");
            int col = i % 2;
            int row = i / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                int current = AutoAnvilClient.CONTROLLER.requested().getOrDefault(entry, 0);
                if (current <= 0) AutoAnvilClient.CONTROLLER.addRequested(entry, 1);
                else if (current < entry.value().getMaxLevel()) AutoAnvilClient.CONTROLLER.addRequested(entry, current + 1);
                else AutoAnvilClient.CONTROLLER.removeRequested(entry);
                rebuild();
            }).dimensions(width / 2 - 155 + col * 160, 40 + row * 27, 150, 22).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page--; rebuild(); })
            .dimensions(width / 2 - 154, height - 55, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; rebuild(); })
            .dimensions(width / 2 + 124, height - 55, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            AutoAnvilClient.CONTROLLER.clearRequested(); rebuild();
        }).dimensions(width / 2 - 115, height - 28, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(width / 2 + 45, height - 28, 70, 20).build());
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
            Text.literal("Selected: " + AutoAnvilClient.CONTROLLER.requested().size()), 10, height - 52, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
