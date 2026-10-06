package com.hiroto99.hardnight.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class FreezingEffect extends MobEffect {

    public FreezingEffect() {
        super(MobEffectCategory.HARMFUL, 0x9FE4FF); // 氷色
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // NeoForgeではこれが必須
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        boolean result = super.applyEffectTick(level, entity, amplifier);
        if (entity.canFreeze()) {
            int add = (amplifier + 1) * 3;
            entity.setIsInPowderSnow(true);
            entity.setTicksFrozen(
                    Math.min(
                            entity.getTicksRequiredToFreeze(),
                            entity.getTicksFrozen() + add
                    )
            );
        }
        return result;
    }
}
