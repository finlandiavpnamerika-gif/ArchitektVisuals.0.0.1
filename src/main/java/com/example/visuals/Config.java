package com.example.visuals;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    // Fullbright
    public static boolean fullbright = false;
    public static double gamma = 10.0;
    // Зум
    public static boolean zoom = true;
    public static double zoomFov = 30;
    // HUD
    public static boolean hud = true;
    public static boolean hudFps = true;
    public static boolean hudPing = true;
    public static boolean hudCoords = true;
    public static boolean hudFacing = true;
    public static boolean hudEffects = true;
    public static double hudScale = 1.0;
    public static int hudX = 4;
    public static int hudY = 4;
    public static int hudColor = 0;
    // Авто-спринт
    public static boolean sprint = false;
    public static boolean sprintAllDirs = false;
    // Лимит дальности
    public static boolean limitDist = false;
    public static double viewDist = 8;

    public static final int[] COLORS = {0xFFB57CFF, 0xFF55FFFF, 0xFF55FF55, 0xFFFFFF55, 0xFFFF5555};
    public static final String[] COLOR_NAMES = {"Фиолетовый", "Голубой", "Зелёный", "Жёлтый", "Красный"};

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visualsmod.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("fullbright", fullbright);
            o.addProperty("gamma", gamma);
            o.addProperty("zoom", zoom);
            o.addProperty("zoomFov", zoomFov);
            o.addProperty("hud", hud);
            o.addProperty("hudFps", hudFps);
            o.addProperty("hudPing", hudPing);
            o.addProperty("hudCoords", hudCoords);
            o.addProperty("hudFacing", hudFacing);
            o.addProperty("hudEffects", hudEffects);
            o.addProperty("hudScale", hudScale);
            o.addProperty("hudX", hudX);
            o.addProperty("hudY", hudY);
            o.addProperty("hudColor", hudColor);
            o.addProperty("sprint", sprint);
            o.addProperty("sprintAllDirs", sprintAllDirs);
            o.addProperty("limitDist", limitDist);
            o.addProperty("viewDist", viewDist);
            Files.writeString(FILE, GSON.toJson(o));
        } catch (Exception ignored) {
        }
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject o = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            fullbright = b(o, "fullbright", fullbright);
            gamma = d(o, "gamma", gamma);
            zoom = b(o, "zoom", zoom);
            zoomFov = d(o, "zoomFov", zoomFov);
            hud = b(o, "hud", hud);
            hudFps = b(o, "hudFps", hudFps);
            hudPing = b(o, "hudPing", hudPing);
            hudCoords = b(o, "hudCoords", hudCoords);
            hudFacing = b(o, "hudFacing", hudFacing);
            hudEffects = b(o, "hudEffects", hudEffects);
            hudScale = d(o, "hudScale", hudScale);
            hudX = (int) d(o, "hudX", hudX);
            hudY = (int) d(o, "hudY", hudY);
            hudColor = Math.max(0, Math.min(COLORS.length - 1, (int) d(o, "hudColor", hudColor)));
            sprint = b(o, "sprint", sprint);
            sprintAllDirs = b(o, "sprintAllDirs", sprintAllDirs);
            limitDist = b(o, "limitDist", limitDist);
            viewDist = Math.max(2, Math.min(32, d(o, "viewDist", viewDist)));
        } catch (Exception ignored) {
        }
    }

    private static boolean b(JsonObject o, String k, boolean def) {
        return o.has(k) ? o.get(k).getAsBoolean() : def;
    }

    private static double d(JsonObject o, String k, double def) {
        return o.has(k) ? o.get(k).getAsDouble() : def;
    }
              }
