package com.example.visuals;

import com.example.visuals.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** AutoSwap (пары предметов в левую руку) и ItemBind (предмет на клавишу). */
public class Binds {
    public static class Swap {
        public final String a, b;
        public int key;
        boolean wasDown;

        Swap(String a, String b, int key) {
            this.a = a;
            this.b = b;
            this.key = key;
        }
    }

    public static class Bind {
        public final String name;
        public int key;
        boolean wasDown;

        Bind(String name, int key) {
            this.name = name;
            this.key = key;
        }
    }

    public static final List<Swap> swaps = new ArrayList<>();
    public static final List<Bind> binds = new ArrayList<>();

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visualsmod_binds.txt");
    private static final long COOLDOWN_MS = 50;

    private static String swapFirst;
    private static String[] pendingSwap;
    private static String pendingBind;

    private static int returnSlot = -1;
    private static int returnHotbar;
    private static long returnAt;
    private static long lastSwap;

    private static boolean prevSwapSel, prevBindSel;
    private static final boolean[] prevKeys = new boolean[350];

    // ---------- сохранение ----------
    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length == 4 && p[0].equals("S")) swaps.add(new Swap(p[2], p[3], Integer.parseInt(p[1])));
                else if (p.length == 3 && p[0].equals("B")) binds.add(new Bind(p[2], Integer.parseInt(p[1])));
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            List<String> out = new ArrayList<>();
            for (Swap s : swaps) out.add("S\t" + s.key + "\t" + s.a + "\t" + s.b);
            for (Bind b : binds) out.add("B\t" + b.key + "\t" + b.name);
            Files.write(FILE, out, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    // ---------- утилиты ----------
    public static String keyName(int k) {
        String n = GLFW.glfwGetKeyName(k, 0);
        if (n != null) return n.toUpperCase();
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        return switch (k) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            default -> "K" + k;
        };
    }

    private static String name(ItemStack s) {
        return s.getName().getString().trim();
    }

    private static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal(s), true);
    }

    private static int code(net.minecraft.client.option.KeyBinding kb) {
        return KeyBindingHelper.getBoundKeyOf(kb).getCode();
    }

    private static void snapshot(long h) {
        for (int k = 32; k < 349; k++) prevKeys[k] = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
    }

    private static String hovered(MinecraftClient mc) {
        if (!(mc.currentScreen instanceof HandledScreenAccessor acc)) return null;
        Slot s = acc.getFocusedSlot();
        if (s == null || !s.hasStack()) return null;
        return name(s.getStack());
    }

    /** -1 если не найден, иначе id слота для окна инвентаря игрока. */
    private static int findSlotId(MinecraftClient mc, String name) {
        for (int i = 0; i < 36; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (!st.isEmpty() && name(st).equalsIgnoreCase(name)) return i < 9 ? 36 + i : i;
        }
        return -1;
    }

    private static int findInventoryIndex(MinecraftClient mc, String name) {
        for (int i = 0; i < 36; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (!st.isEmpty() && name(st).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private static void click(MinecraftClient mc, int slot, int button) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, button,
                SlotActionType.SWAP, mc.player);
    }

    // ---------- главный тик ----------
    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null || mc.getWindow() == null) return;
        long now = System.currentTimeMillis();
        long h = mc.getWindow().getHandle();

        // возврат предмета (ItemBind)
        if (returnSlot != -1 && now >= returnAt
                && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            click(mc, returnSlot, returnHotbar);
            returnSlot = -1;
        }

        boolean inInv = mc.currentScreen instanceof InventoryScreen;

        // ожидание клавиши для нового бинда
        if (pendingSwap != null || pendingBind != null) {
            if (!inInv) {
                pendingSwap = null;
                pendingBind = null;
                swapFirst = null;
            } else {
                int pressed = -1;
                for (int k = 32; k < 349; k++) {
                    boolean p = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
                    if (p && !prevKeys[k] && pressed == -1) pressed = k;
                    prevKeys[k] = p;
                }
                if (pressed != -1) assign(mc, pressed);
            }
            return;
        }
        if (!inInv) swapFirst = null;

        // выбор предмета в инвентаре
        if (inInv) {
            int sc = code(VisualsClient.swapKey);
            boolean ds = sc > 0 && GLFW.glfwGetKey(h, sc) == GLFW.GLFW_PRESS;
            if (ds && !prevSwapSel && Config.autoSwap) selectSwap(mc, h);
            prevSwapSel = ds;

            int bc = code(VisualsClient.bindKey);
            boolean db = bc > 0 && GLFW.glfwGetKey(h, bc) == GLFW.GLFW_PRESS;
            if (db && !prevBindSel && Config.itemBind) selectBind(mc, h);
            prevBindSel = db;
        } else {
            prevSwapSel = false;
            prevBindSel = false;
        }

        boolean ok = mc.currentScreen == null || inInv;
        if (!ok) {
            for (Swap s : swaps) s.wasDown = s.key != -1 && GLFW.glfwGetKey(h, s.key) == GLFW.GLFW_PRESS;
            for (Bind b : binds) b.wasDown = b.key != -1 && GLFW.glfwGetKey(h, b.key) == GLFW.GLFW_PRESS;
            return;
        }

        for (Swap s : new ArrayList<>(swaps)) {
            boolean down = s.key != -1 && GLFW.glfwGetKey(h, s.key) == GLFW.GLFW_PRESS;
            if (Config.autoSwap && down && !s.wasDown) swapPair(mc, s);
            s.wasDown = down;
        }
        for (Bind b : new ArrayList<>(binds)) {
            boolean down = b.key != -1 && GLFW.glfwGetKey(h, b.key) == GLFW.GLFW_PRESS;
            if (Config.itemBind && mc.currentScreen == null && down && !b.wasDown && returnSlot == -1) {
                useBound(mc, b);
            }
            b.wasDown = down;
        }
    }

    // ---------- выбор ----------
    private static void selectSwap(MinecraftClient mc, long h) {
        String n = hovered(mc);
        if (n == null) return;
        if (swapFirst == null) {
            swapFirst = n;
            msg(mc, "AutoSwap: выбран " + n + ". Наведи на второй предмет и нажми кнопку выбора");
        } else {
            if (swapFirst.equalsIgnoreCase(n)) {
                msg(mc, "AutoSwap: выбери другой предмет");
                return;
            }
            pendingSwap = new String[]{swapFirst, n};
            swapFirst = null;
            snapshot(h);
            msg(mc, "AutoSwap: нажми клавишу для бинда (Esc - отмена)");
        }
    }

    private static void selectBind(MinecraftClient mc, long h) {
        String n = hovered(mc);
        if (n == null) return;
        boolean removed = binds.removeIf(b -> b.name.equalsIgnoreCase(n));
        if (removed) {
            save();
            msg(mc, "ItemBind: бинд " + n + " удалён");
            return;
        }
        pendingBind = n;
        snapshot(h);
        msg(mc, "ItemBind: нажми клавишу для " + n + " (Esc - отмена)");
    }

    private static boolean keyTaken(int k) {
        for (Swap s : swaps) if (s.key == k) return true;
        for (Bind b : binds) if (b.key == k) return true;
        return false;
    }

    private static void assign(MinecraftClient mc, int k) {
        if (k == GLFW.GLFW_KEY_ESCAPE) {
            pendingSwap = null;
            pendingBind = null;
            swapFirst = null;
            msg(mc, "Отменено");
            return;
        }
        if (k == 340 || k == 344 || k == 341 || k == 345 || k == 342 || k == 346) {
            msg(mc, "Shift/Ctrl/Alt нельзя, нажми другую клавишу");
            return;
        }
        if (k == code(VisualsClient.swapKey) || k == code(VisualsClient.bindKey)) {
            msg(mc, "Эта клавиша занята под выбор предметов");
            return;
        }
        if (keyTaken(k)) {
            msg(mc, "Клавиша уже занята");
            return;
        }
        if (pendingSwap != null) {
            swaps.add(new Swap(pendingSwap[0], pendingSwap[1], k));
            msg(mc, "AutoSwap: [" + keyName(k) + "] = " + pendingSwap[0] + " <-> " + pendingSwap[1]);
            pendingSwap = null;
        } else if (pendingBind != null) {
            binds.add(new Bind(pendingBind, k));
            msg(mc, "ItemBind: [" + keyName(k) + "] = " + pendingBind);
            pendingBind = null;
        }
        save();
    }

    // ---------- действия ----------
    private static void swapPair(MinecraftClient mc, Swap p) {
        long now = System.currentTimeMillis();
        if (now - lastSwap < COOLDOWN_MS) return;
        ItemStack off = mc.player.getOffHandStack();
        String cur = off.isEmpty() ? "" : name(off);
        String[] want;
        if (cur.equalsIgnoreCase(p.a)) want = new String[]{p.b};
        else if (cur.equalsIgnoreCase(p.b)) want = new String[]{p.a};
        else want = new String[]{p.a, p.b};
        for (String w : want) {
            int slot = findSlotId(mc, w);
            if (slot != -1) {
                click(mc, slot, 40);
                lastSwap = now;
                return;
            }
        }
        msg(mc, "AutoSwap: предметы пары не найдены");
    }

    private static void useBound(MinecraftClient mc, Bind b) {
        int idx = findInventoryIndex(mc, b.name);
        if (idx == -1) {
            msg(mc, "ItemBind: " + b.name + " не найден");
            return;
        }
        int sel = mc.player.getInventory().getSelectedSlot();
        if (idx != sel) {
            int slot = idx < 9 ? 36 + idx : idx;
            click(mc, slot, sel);
            returnSlot = slot;
            returnHotbar = sel;
            returnAt = System.currentTimeMillis() + COOLDOWN_MS;
        }
        useMainHand(mc);
    }

    private static void useMainHand(MinecraftClient mc) {
        if (mc.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            ActionResult r = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            if (r.isAccepted()) {
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
        ActionResult r2 = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (r2.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
    }
  }
