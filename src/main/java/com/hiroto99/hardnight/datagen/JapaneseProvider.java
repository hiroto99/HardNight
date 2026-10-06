package com.hiroto99.hardnight.datagen;

import com.hiroto99.hardnight.register.ModEntities;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.hiroto99.hardnight.HardNight.MODID;

public class JapaneseProvider extends LanguageProvider {

    public JapaneseProvider(PackOutput output) {
        super(
                // Provided by the `GatherDataEvent.Client`.
                output,
                // Your mod id.
                MODID,
                // The locale to use. You may use multiple language providers for different locales.
                "ja_jp"
        );
    }

    @Override
    protected void addTranslations() {
        this.add(ModEntities.ZOMBIE_FISHING_HOOK.get(), "釣り竿");
        this.add(ModEntities.FROZEN_ZOMBIE.get(), "フローズンゾンビ");
        this.add(ModEntities.FROZEN_ZOMBIE_HORSE.get(), "フローズンゾンビホース");
        this.add(ModEntities.GHAST.get(), "ガスト");
    }
}
