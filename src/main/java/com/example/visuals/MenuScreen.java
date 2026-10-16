package com.example.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class MenuScreen extends Screen {
    private static final int ACC = 0xFFB57CFF;
    private static final int ACC_DIM = 0xFF6A3FB5;
    private static final int BG = 0xE6100C18;
    private static final int ROW = 0xFF1A1426;
    private static final int ROW2 = 0xFF120E1A;
    private static final int OFF = 0xFF4A4458;
    private static final int WHITE = 0xFFFFFFFF;

    // какие модули раскрыты (помним между открытиями)
    private static final Set<String> open = new HashSet<>();

    private int mx, my;
    private boolean down, clicked, prevDown = true;
    private int active = -1, idc;
    private int cx, cy, cw;

    public MenuScreen() {
        super(Text.literal("Visuals"));
    }

    // ---------- хелперы ----------
    private boolean hit(int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void text(DrawContext c, String s, int x, int y, int col) {
        c.drawTextWithShadow(textRenderer, s, x, y, col);
    }

    private void box(DrawContext c, int x, int y, int w, int h, int border, int fill) {
        c.fill(x, y, x + w, y + h, border);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void sw(DrawContext c, int x, int y, boolean on) {
        c.fill(x, y, x + 22, y + 10, on ? ACC : OFF);
        int k = on ? x + 12 : x + 2;
        c.fill(k, y + 2, k + 8, y + 8, WHITE);
    }

    private int mh(String id, int n) {
        return 24 + (open.contains(id) ? n * 20 + 2 : 0);
    }

    private void panel(DrawContext c, String title, int x, int y, int w, int h) {
        box(c, x, y, w, h, ACC_DIM, BG);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(title).formatted(Formatting.BOLD),
                x + w / 2, y + 8, WHITE);
        c.fill(x + 6, y + 22, x + w - 6, y + 24, ACC);
        cx = x + 6;
        cw = w - 12;
        cy = y + 30;
    }

    // ---------- виджеты ----------
    /** Строка модуля: переключатель справа, клик по остальной части раскрывает настройки. */
    private boolean module(DrawContext c, String id, String name, String key, boolean on, int n) {
        boolean over = hit(cx, cy, cw, 22);
        c.fill(cx, cy, cx + cw, cy + 22, over ? 0xFF241B38 : ROW);
        text(c, name + (key.isEmpty() ? "" : " [" + key + "]"), cx + 5, cy + 7, WHITE);
        if (n > 0) text(c, open.contains(id) ? "^" : "v", cx + cw - 38, cy + 7, 0xFFAAAAAA);
        sw(c, cx + cw - 28, cy + 6, on);
        if (clicked && over) {
            if (mx >= cx + cw - 32) on = !on;
            else if (n > 0) {
                if (!open.remove(id)) open.add(id);
            }
        }
        cy += 24;
        return on;
    }

    private boolean cbool(DrawContext c, String label, boolean v) {
        boolean over = hit(cx, cy, cw, 18);
        c.fill(cx, cy, cx + cw, cy + 18, over ? 0xFF1B1428 : ROW2);
        text(c, label, cx + 5, cy + 5, 0xFFDDDDDD);
        sw(c, cx + cw - 28, cy + 4, v);
        if (clicked && over) v = !v;
        cy += 20;
        return v;
    }

    private double cslider(DrawContext c, String label, double min, double max, double v, boolean integer) {
        int id = idc++;
        boolean over = hit(cx, cy, cw, 18);
        if (clicked && over) active = id;
        if (active == id && down) {
            double f = Math.max(0, Math.min(1, (mx - cx) / (double) cw));
            v = min + f * (max - min);
            v = integer ? Math.round(v) : Math.round(v * 10) / 10.0;
        }
        double frac = (v - min) / (max - min);
        c.fill(cx, cy, cx + cw, cy + 18, ROW2);
        c.fill(cx, cy, cx + (int) (cw * frac), cy + 18, ACC_DIM);
        String val = integer ? String.valueOf((int) v) : String.valueOf(v);
        c.drawCenteredTextWithShadow(textRenderer, label + ": " + val, cx + cw / 2, cy + 5, WHITE);
        cy += 20;
        return v;
    }

    private int ccycle(DrawContext c, String label, String[] names, int v) {
        boolean over = hit(cx, cy, cw, 18);
        c.fill(cx, cy, cx + cw, cy + 18, over ? 0xFF1B1428 : ROW2);
        text(c, label + ": " + names[v], cx + 5, cy + 5, 0xFFDDDDDD);
        c.fill(cx + cw - 16, cy + 4, cx + cw - 6, cy + 14, Config.COLORS[Math.min(v, Config.COLORS.length - 1)]);
        if (clicked && over) v = (v + 1) % names.length;
        cy += 20;
        return v;
    }

    // ---------- отрисовка ----------
    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        mx = mouseX;
        my = mouseY;
        long win = client.getWindow().getHandle();
        down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        clicked = down && !prevDown;
        prevDown = down;
        if (!down) active = -1;
        idc = 0;

        c.fill(0, 0, width, height, 0x90000000);

        int pw = Math.max(90, Math.min(150, (width - 30) / 3));
        int gap = 10;
        int x0 = (width - (pw * 3 + gap * 2)) / 2;
        int top = 14;

        // --- Utilities ---
        int h1 = 34 + mh("sprint", 1) + mh("zoom", 1);
        panel(c, "Utilities", x0, top, pw, h1);
        Config.sprint = module(c, "sprint", "AutoSprint", "", Config.sprint, 1);
        if (open.contains("sprint")) {
            Config.sprintAllDirs = cbool(c, "Все стороны", Config.sprintAllDirs);
            cy += 2;
        }
        Config.zoom = module(c, "zoom", "Zoom", "C", Config.zoom, 1);
        if (open.contains("zoom")) {
            Config.zoomFov = cslider(c, "FOV", 10, 60, Config.zoomFov, true);
            cy += 2;
        }

        // --- Visuals ---
        int h2 = 34 + mh("fb", 1) + mh("hud", 9);
        panel(c, "Visuals", x0 + pw + gap, top, pw, h2);
        Config.fullbright = module(c, "fb", "Fullbright", "", Config.fullbright, 1);
        if (open.contains("fb")) {
            Config.gamma = cslider(c, "Яркость", 1, 15, Config.gamma, false);
            cy += 2;
        }
        Config.hud = module(c, "hud", "HUD", "", Config.hud, 9);
        if (open.contains("hud")) {
            Config.hudFps = cbool(c, "FPS", Config.hudFps);
            Config.hudPing = cbool(c, "Ping", Config.hudPing);
            Config.hudCoords = cbool(c, "Координаты", Config.hudCoords);
            Config.hudFacing = cbool(c, "Направление", Config.hudFacing);
            Config.hudEffects = cbool(c, "Эффекты", Config.hudEffects);
            Config.hudScale = cslider(c, "Размер", 0.5, 3, Config.hudScale, false);
            Config.hudColor = ccycle(c, "Цвет", Config.COLOR_NAMES, Config.hudColor);
            Config.hudX = (int) cslider(c, "Позиция X", 0, 400, Config.hudX, true);
            Config.hudY = (int) cslider(c, "Позиция Y", 0, 250, Config.hudY, true);
            cy += 2;
        }

        // --- Performance ---
        int h3 = 34 + mh("dist", 1);
        panel(c, "Performance", x0 + (pw + gap) * 2, top, pw, h3);
        Config.limitDist = module(c, "dist", "Лимит дальности", "", Config.limitDist, 1);
        if (open.contains("dist")) {
            Config.viewDist = cslider(c, "Чанки", 2, 32, Config.viewDist, true);
            cy += 2;
        }

        // --- Низ: закрыть ---
        int fw = 150, fh = 22;
        int fx = (width - fw) / 2, fy = height - fh - 8;
        boolean over = hit(fx, fy, fw, fh);
        box(c, fx, fy, fw, fh, over ? ACC : ACC_DIM, BG);
        c.drawCenteredTextWithShadow(textRenderer, "Visuals  |  Закрыть", width / 2, fy + 7, WHITE);
        if (clicked && over) close();
    }

    @Override
    public void removed() {
        Config.save();
    }
                           }
