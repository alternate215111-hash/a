package com.example.itemsway;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ItemSwayMod.MODID)
public class ItemSwayMod {
    public static final String MODID = "itemsway";

    public ItemSwayMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SwayConfig.SPEC);
    }
}
