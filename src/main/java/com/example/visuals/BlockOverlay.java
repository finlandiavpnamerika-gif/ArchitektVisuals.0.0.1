package com.example.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/** Подсветка грани блока, на который смотришь (рисуется поверх экрана). */
public class BlockOverlay {

    public static void render(DrawContext c, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.blockOverlay || mc.player == null || mc.world == null
                || mc.currentScreen != null || mc.options.hudHidden) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        float t = tc.getTickProgress(false);
        BlockPos pos = hit.getBlockPos();

        Box box;
        VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) box = new Box(0, 0, 0, 1, 1, 1);
        else box = shape.getBoundingBox();
        box = box.offset(pos);

        // углы грани, в которую смотрим
        Direction d = hit.getSide();
        double[][] cs;
        if (d.getOffsetX() != 0) {
            double x = d.getOffsetX() > 0 ? box.maxX : box.minX;
            cs = new double[][]{{x, box.minY, box.minZ}, {x, box.maxY, box.minZ},
                    {x, box.maxY, box.maxZ}, {x, box.minY, box.maxZ}};
        } else if (d.getOffsetY() != 0) {
            double y = d.getOffsetY() > 0 ? box.maxY : box.minY;
            cs = new double[][]{{box.minX, y, box.minZ}, {box.maxX, y, box.minZ},
                    {box.maxX, y, box.maxZ}, {box.minX, y, box.maxZ}};
        } else {
            double z = d.getOffsetZ() > 0 ? box.maxZ : box.minZ;
            cs = new double[][]{{box.minX, box.minY, z}, {box.maxX, box.minY, z},
                    {box.maxX, box.maxY, z}, {box.minX, box.maxY, z}};
        }

        // камера
        Vec3d cam = mc.player.getLerpedPos(t).add(0, mc.player.getEyeY() - mc.player.getY(), 0);
        double yaw = Math.toRadians(mc.player.getYaw(t));
        double pitch = Math.toRadians(mc.player.getPitch(t));
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) w / h;

        double[][] s = new double[4][2];
        for (int i = 0; i < 4; i++) {
            double dx = cs[i][0] - cam.x, dy = cs[i][1] - cam.y, dz = cs[i][2] - cam.z;
            double z = dx * fx + dy * fy + dz * fz;
            if (z < 0.05) return; // за камерой
            double x = dx * rx + dy * ry + dz * rz;
            double y = dx * ux + dy * uy + dz * uz;
            s[i][0] = w / 2.0 + (x / z) / (tanHalf * aspect) * (w / 2.0);
            s[i][1] = h / 2.0 - (y / z) / tanHalf * (h / 2.0);
        }

        int rgb = Config.COLORS[Config.boColor] & 0xFFFFFF;
        if (Config.boFill) {
            int a = (int) (Config.boAlpha * 2.55);
            fillPoly(c, s, w, h, (a << 24) | rgb);
        }
        if (Config.boOutline) {
            int th = (int) Config.boThick;
            for (int i = 0; i < 4; i++) {
                double[] p = s[i], q = s[(i + 1) % 4];
                line(c, p[0], p[1], q[0], q[1], th, 0xFF000000 | rgb);
            }
        }
    }

    private static void line(DrawContext c, double x0, double y0, double x1, double y1, int th, int col) {
        double dx = x1 - x0, dy = y1 - y0;
        int steps = (int) Math.min(3000, Math.max(Math.abs(dx), Math.abs(dy)));
        if (steps <= 0) {
            c.fill((int) x0, (int) y0, (int) x0 + th, (int) y0 + th, col);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = (int) (x0 + dx * i / steps);
            int y = (int) (y0 + dy * i / steps);
            c.fill(x, y, x + th, y + th, col);
        }
    }

    private static void fillPoly(DrawContext c, double[][] p, int w, int h, int col) {
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] v : p) {
            minY = Math.min(minY, v[1]);
            maxY = Math.max(maxY, v[1]);
        }
        int y0 = Math.max(0, (int) Math.floor(minY));
        int y1 = Math.min(h - 1, (int) Math.ceil(maxY));
        for (int y = y0; y <= y1; y++) {
            double yy = y + 0.5, xl = Double.MAX_VALUE, xr = -Double.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                double[] a = p[i], b = p[(i + 1) % 4];
                if (a[1] == b[1]) continue;
                if ((yy >= a[1] && yy <= b[1]) || (yy >= b[1] && yy <= a[1])) {
                    double x = a[0] + (yy - a[1]) * (b[0] - a[0]) / (b[1] - a[1]);
                    xl = Math.min(xl, x);
                    xr = Math.max(xr, x);
                }
            }
            if (xl <= xr) {
                int xa = Math.max(0, (int) xl);
                int xb = Math.min(w, (int) xr + 1);
                if (xb > xa) c.fill(xa, y, xb, y + 1, col);
            }
        }
    }
                      }
