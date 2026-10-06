package com.hiroto99.hardnight.datagen;

import com.hiroto99.hardnight.register.ModEntities;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.hiroto99.hardnight.HardNight.MODID;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(
                // Provided by the `GatherDataEvent.Client`.
                output,
                // Your mod id.
                MODID,
                // The locale to use. You may use multiple language providers for different locales.
                "en_us"
        );
    }

    @Override
    protected void addTranslations() {
        this.add(ModEntities.ZOMBIE_FISHING_HOOK.get(), "Fishing Hook");
        this.add(ModEntities.FROZEN_ZOMBIE.get(), "Frozen Zombie");
        this.add(ModEntities.FROZEN_ZOMBIE_HORSE.get(), "Frozen Zombie Horse");
        this.add(ModEntities.GHAST.get(), "Ghast");
    }
}

