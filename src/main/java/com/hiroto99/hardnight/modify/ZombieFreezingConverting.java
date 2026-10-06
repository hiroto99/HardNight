package com.hiroto99.hardnight.modify;

import com.hiroto99.hardnight.register.ModEntities;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import com.hiroto99.hardnight.entity.FrozenZombie;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber // ★ イベントバスに自動登録するために必須
public class ZombieFreezingConverting {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        // ゾンビ（通常のゾンビ）を対象とする場合
        if (event.getEntity() instanceof Zombie zombie && !(zombie instanceof FrozenZombie)) {
            Level level = zombie.level();

            // サーバー側のみ処理実行
            if (!level.isClientSide()) {
                // 粉雪の中に足が入っているか確認
                if (zombie.isInPowderSnow) {
                    // 完全凍結までのカウント（140 tick）に到達したか判定
                    if (zombie.getTicksFrozen() >= zombie.getTicksRequiredToFreeze()) {
                        doFreezeConversion(zombie);
                    }
                }
            }
        }
    }

    static void doFreezeConversion(Zombie zombie) {
        if (EventHooks.canLivingConvert(zombie, ModEntities.FROZEN_ZOMBIE.get(), (_) -> {})) {
            zombie.convertTo(ModEntities.FROZEN_ZOMBIE.get(), ConversionParams.single(zombie, true, true), (frozenZombie) -> {
                EventHooks.onLivingConvert(zombie, frozenZombie);
                if (!zombie.isSilent()) {
                    zombie.level().levelEvent((Entity)null, 1048, zombie.blockPosition(), 0);
                }
            });
        }
    }
}