hudEffects = b(o, "hudEffects", hudEffects);
            hudScale = d(o, "hudScale", hudScale);
            hudX = (int) d(o, "hudX", hudX);
            hudY = (int) d(o, "hudY", hudY);
            hudColor = clampColor((int) d(o, "hudColor", hudColor));
            sprint = b(o, "sprint", sprint);
            sprintAllDirs = b(o, "sprintAllDirs", sprintAllDirs);
            limitDist = b(o, "limitDist", limitDist);
            viewDist = Math.max(2, Math.min(32, d(o, "viewDist", viewDist)));
            autoSwap = b(o, "autoSwap", autoSwap);
            itemBind = b(o, "itemBind", itemBind);
            blockOverlay = b(o, "blockOverlay", blockOverlay);
            boOutline = b(o, "boOutline", boOutline);
            boFill = b(o, "boFill", boFill);
            boColor = clampColor((int) d(o, "boColor", boColor));
            boAlpha = Math.max(0, Math.min(100, d(o, "boAlpha", boAlpha)));
            boThick = Math.max(1, Math.min(4, d(o, "boThick", boThick)));
        } catch (Exception ignored) {
        }
    }

    private static int clampColor(int v) {
        return Math.max(0, Math.min(COLORS.length - 1, v));
    }

    private static boolean b(JsonObject o, String k, boolean def) {
        return o.has(k) ? o.get(k).getAsBoolean() : def;
    }

    private static double d(JsonObject o, String k, double def) {
        return o.has(k) ? o.get(k).getAsDouble() : def;
    }
}
