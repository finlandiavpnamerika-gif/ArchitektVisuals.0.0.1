package com.example.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Features {
    private static double origGamma = -1;
    private static int origFov = -1;
    private static int origView = -1;

    public static void tick(MinecraftClient mc) {
        if (mc.options == null) return;

        // Fullbright
        var gammaOpt = mc.options.getGamma();
        if (Config.fullbright) {
            if (origGamma < 0) origGamma = gammaOpt.getValue();
            if (gammaOpt.getValue() != Config.gamma) gammaOpt.setValue(Config.gamma);
        } else if (origGamma >= 0) {
            gammaOpt.setValue(origGamma);
            origGamma = -1;
        }

        // Зум
        var fovOpt = mc.options.getFov();
        boolean wantZoom = Config.zoom && VisualsClient.zoomKey.isPressed() && mc.currentScreen == null;
        if (wantZoom) {
            if (origFov < 0) origFov = fovOpt.getValue();
            int target = (int) Config.zoomFov;
            if (fovOpt.getValue() != target) fovOpt.setValue(target);
        } else if (origFov >= 0) {
            fovOpt.setValue(origFov);
            origFov = -1;
        }

        // Лимит дальности
        var viewOpt = mc.options.getViewDistance();
        if (Config.limitDist) {
            if (origView < 0) origView = viewOpt.getValue();
            int target = (int) Config.viewDist;
            if (viewOpt.getValue() != target) viewOpt.setValue(target);
        } else if (origView >= 0) {
            viewOpt.setValue(origView);
            origView = -1;
        }

        // Авто-спринт
        if (mc.player != null && Config.sprint && mc.currentScreen == null
                && !mc.player.isSneaking() && !mc.player.isUsingItem()
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            boolean fwd = mc.options.forwardKey.isPressed();
            boolean any = fwd || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
            if (Config.sprintAllDirs ? any : fwd) mc.player.setSprinting(true);
        }
    }

    public static void restore(MinecraftClient mc) {
        try {
            if (origGamma >= 0) mc.options.getGamma().setValue(origGamma);
            if (origFov >= 0) mc.options.getFov().setValue(origFov);
            if (origView >= 0) mc.options.getViewDistance().setValue(origView);
        } catch (Exception ignored) {
        }
        origGamma = -1;
        origFov = -1;
        origView = -1;
    }

    private static String time(StatusEffectInstance e) {
        if (e.isInfinite()) return "inf";
        int s = e.getDuration() / 20;
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    public static void hud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.hud || mc.player == null || mc.options.hudHidden) return;

        TextRenderer tr = mc.textRenderer;
        List<String> lines = new ArrayList<>();
        if (Config.hudFps) lines.add("FPS: " + mc.getCurrentFps());
        if (Config.hudPing) {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null
                    : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            lines.add("PING: " + (e == null ? 0 : e.getLatency()) + "ms");
        }
        if (Config.hudCoords) {
            lines.add("Coords:");
            lines.add(String.format(Locale.ROOT, "X: %.1f / Y: %.1f / Z: %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
        if (Config.hudFacing) {
            String f = mc.player.getHorizontalFacing().asString();
            lines.add("(Facing " + Character.toUpperCase(f.charAt(0)) + f.substring(1) + ")");
        }
        List<String> eff = new ArrayList<>();
        if (Config.hudEffects) {
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String n = e.getEffectType().value().getName().getString();
                if (e.getAmplifier() > 0) n += " " + (e.getAmplifier() + 1);
                eff.add(n + " (" + time(e) + ")");
            }
        }

        String title = "VisualsMod v1.1";
        int w = tr.getWidth(title);
        for (String s : lines) w = Math.max(w, tr.getWidth(s));
        for (String s : eff) w = Math.max(w, tr.getWidth(s));
        int h = 14 + lines.size() * 10 + (eff.isEmpty() ? 0 : 14 + eff.size() * 10) + 4;

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) Config.hudX, (float) Config.hudY);
        m.scale((float) Config.hudScale, (float) Config.hudScale);

        int accent = Config.COLORS[Config.hudColor];
        ctx.fill(0, 0, w + 10, h, 0x90000000);
        ctx.fill(0, 0, 2, h, accent);
        int y = 3;
        ctx.drawTextWithShadow(tr, title, 6, y, accent);
        y += 12;
        for (String s : lines) {
            ctx.drawTextWithShadow(tr, s, 6, y, 0xFFFFFFFF);
            y += 10;
        }
        if (!eff.isEmpty()) {
            y += 3;
            ctx.drawTextWithShadow(tr, "POTION EFFECTS", 6, y, accent);
            y += 11;
            for (String s : eff) {
                ctx.drawTextWithShadow(tr, s, 6, y, 0xFFFFFFFF);
                y += 10;
            }
        }
        m.popMatrix();
    }
            }
