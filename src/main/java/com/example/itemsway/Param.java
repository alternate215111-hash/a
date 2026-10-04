package com.example.itemsway;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Locale;

/** One tweakable setting (number slider or on/off toggle). */
public final class Param {
    public final SwayParams.Cat cat;
    public final String id, label, desc, unit;
    public final double def, min, max, step;
    public final boolean bool;
    public final int decimals;

    /** Live value used by the simulation. */
    public double value;
    /** Backing config entry (set up in SwayConfig). */
    public ForgeConfigSpec.ConfigValue<?> cfg;

    public Param(SwayParams.Cat cat, String id, String label, String unit, double def,
                 double min, double max, double step, boolean bool, String desc) {
        this.cat = cat;
        this.id = id;
        this.label = label;
        this.unit = unit;
        this.def = def;
        this.min = min;
        this.max = max;
        this.step = step;
        this.bool = bool;
        this.desc = desc;
        this.value = def;
        this.decimals = bool ? 0 : Math.max(0, (int) Math.ceil(-Math.log10(step) - 1e-9));
    }

    public double v() { return value; }
    public boolean on() { return value > 0.5; }

    public double clamp(double x) { return Math.max(min, Math.min(max, x)); }

    public double toNorm(double x) {
        return max == min ? 0 : (clamp(x) - min) / (max - min);
    }

    public double fromNorm(double n) {
        double raw = min + n * (max - min);
        double q = min + Math.round((raw - min) / step) * step;
        double scale = Math.pow(10, decimals + 1);
        return clamp(Math.round(q * scale) / scale);
    }

    public String format(double x) {
        if (bool) return x > 0.5 ? "ON" : "OFF";
        if (Math.abs(x) < 1e-9) x = 0;
        return String.format(Locale.ROOT, "%." + decimals + "f", x) + unit;
    }
}
