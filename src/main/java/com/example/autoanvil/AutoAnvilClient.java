package com.example.autoanvil;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class AutoAnvilClient implements ClientModInitializer {
    public static final AutoEnchantController CONTROLLER = new AutoEnchantController();
    public static KeyBinding OPEN_CONFIG;
    public static KeyBinding TOGGLE_AUTO;
    public static KeyBinding START_AUTO;

    @Override
    public void onInitializeClient() {
        CONTROLLER.init();
        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autoanvil.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.autoanvil"));
        TOGGLE_AUTO = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autoanvil.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.autoanvil"));
        START_AUTO = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autoanvil.start", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.autoanvil"));

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_CONFIG.wasPressed()) {
                if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.AnvilScreen) {
                    client.setScreen(new com.example.autoanvil.ui.AutoAnvilConfigScreen(client.currentScreen));
                }
            }
            while (TOGGLE_AUTO.wasPressed()) CONTROLLER.toggle();
            while (START_AUTO.wasPressed()) {
                if (CONTROLLER.isRunning()) CONTROLLER.stop(); else CONTROLLER.start(client);
            }
        });
    }
}
