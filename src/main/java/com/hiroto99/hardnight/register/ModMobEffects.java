package com.hiroto99.hardnight.register;

import com.hiroto99.hardnight.effect.FreezingEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.hiroto99.hardnight.HardNight.MODID;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MODID);

    public static final Holder<MobEffect> FREEZING =
            EFFECTS.register("freezing", FreezingEffect::new);

    public static void init(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
