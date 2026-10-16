package com.example.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VisualsClient implements ClientModInitializer {
    public static KeyBinding menuKey;
    public static KeyBinding zoomKey;
    public static KeyBinding swapKey;
    public static KeyBinding bindKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        Binds.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("visualsmod", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visualsmod.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visualsmod.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visualsmod.swap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
        bindKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visualsmod.bind", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.wasPressed()) {
                mc.setScreen(new MenuScreen());
            }
            Features.tick(mc);
            Binds.tick(mc);
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of("visualsmod", "overlay"),
                BlockOverlay::render);
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of("visualsmod", "hud"),
                Features::hud);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("visuals").executes(ctx -> {
                    var mc = ctx.getSource().getClient();
                    mc.send(() -> mc.setScreen(new MenuScreen()));
                    return 1;
                })));

        ClientLifecycleEvents.CLIENT_STOPPING.register(Features::restore);
    }
}
