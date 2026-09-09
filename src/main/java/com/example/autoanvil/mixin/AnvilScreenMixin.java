package com.example.autoanvil.mixin;

import com.example.autoanvil.AutoAnvilClient;
import com.example.autoanvil.ui.AutoAnvilConfigScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends HandledScreen<AnvilScreenHandler> {
    protected AnvilScreenMixin(AnvilScreenHandler handler, net.minecraft.entity.player.PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void autoanvil$setup(CallbackInfo ci) {
        int bx = this.x + 145;
        int by = this.y + 4;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Auto"), b -> {
            AutoAnvilClient.CONTROLLER.toggle();
            b.setMessage(Text.literal("Auto " + (AutoAnvilClient.CONTROLLER.isEnabled() ? "ON" : "OFF")));
        }).dimensions(bx, by, 55, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Config"), b -> {
            if (this.client != null) this.client.setScreen(new AutoAnvilConfigScreen((AnvilScreen)(Object)this));
        }).dimensions(bx + 60, by, 55, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), b -> {
            if (AutoAnvilClient.CONTROLLER.isRunning()) AutoAnvilClient.CONTROLLER.stop();
            else AutoAnvilClient.CONTROLLER.start(this.client);
        }).dimensions(bx + 120, by, 55, 20).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void autoanvil$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        String status = AutoAnvilClient.CONTROLLER.status();
        context.drawText(this.textRenderer, Text.literal(status), this.x + 145, this.y + 29, 0xFFFFFF, false);
        int cost = this.handler.getLevelCost();
        if (cost > 0) {
            context.drawText(this.textRenderer, Text.literal("Cost: " + cost), this.x + 145, this.y + 41, 0xFFFFFF, false);
        }
    }
}
