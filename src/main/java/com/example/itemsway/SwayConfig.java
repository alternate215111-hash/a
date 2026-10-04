package com.example.itemsway;

import net.minecraftforge.common.ForgeConfigSpec;

/** Builds the config file from the parameter list and syncs values both ways. */
public final class SwayConfig {
    private SwayConfig() {}

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SwayParams.Cat open = null;
        for (Param p : SwayParams.ALL) {
            if (p.cat != open) {
                if (open != null) b.pop();
                b.push(p.cat.id());
                open = p.cat;
            }
            String comment = p.desc + (p.bool
                    ? " (default " + p.format(p.def) + ")"
                    : " (default " + p.format(p.def) + ", range " + p.format(p.min) + " to " + p.format(p.max) + ")");
            if (p.bool) {
                p.cfg = b.comment(comment).define(p.id, p.def > 0.5);
            } else {
                p.cfg = b.comment(comment).defineInRange(p.id, p.def, p.min, p.max);
            }
        }
        if (open != null) b.pop();
        SPEC = b.build();
    }

    /** Config file -> live values. */
    public static void pull() {
        for (Param p : SwayParams.ALL) {
            try {
                Object o = p.cfg.get();
                if (o instanceof Boolean bo) p.value = bo ? 1 : 0;
                else if (o instanceof Number n) p.value = p.clamp(n.doubleValue());
            } catch (IllegalStateException ignored) {
                // config not loaded yet
            }
        }
    }

    /** Live values -> config file. */
    public static void save() {
        try {
            for (Param p : SwayParams.ALL) {
                if (p.bool) ((ForgeConfigSpec.BooleanValue) p.cfg).set(p.on());
                else ((ForgeConfigSpec.DoubleValue) p.cfg).set(p.value);
            }
            SPEC.save();
        } catch (RuntimeException ignored) {
            // config not available; values still apply for this session
        }
    }
}
