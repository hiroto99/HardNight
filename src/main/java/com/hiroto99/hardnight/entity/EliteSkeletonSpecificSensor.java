package com.hiroto99.hardnight.entity;

import com.google.common.collect.ImmutableSet;
import com.hiroto99.hardnight.register.ModBrains;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.Optional;
import java.util.Set;

import static com.hiroto99.hardnight.HardNight.LOGGER;

public class EliteSkeletonSpecificSensor extends Sensor<EliteSkeleton> {
    public EliteSkeletonSpecificSensor() {
        // スキャン間隔を 20 から 2〜5 ティックに変更（即座に接近を検知するため）
        super(2);
    }

    @Override
    protected void doTick(ServerLevel level, EliteSkeleton entity) {
        Brain<EliteSkeleton> brain = entity.getBrain();

        // 1. 盾破壊クールダウンの減衰処理（周期が2ティックになったため、2減算）
        Optional<Integer> cooldownOpt = brain.getMemory(ModBrains.SHIELD_DISABLED_COOLDOWN.get());
        if (cooldownOpt.isPresent()) {
            int newCooldown = cooldownOpt.get() - 2;
            if (newCooldown > 0) {
                brain.setMemory(ModBrains.SHIELD_DISABLED_COOLDOWN.get(), newCooldown);
            } else {
                brain.eraseMemory(ModBrains.SHIELD_DISABLED_COOLDOWN.get());
            }
        }

        // 2. 距離判定によるモード切替
        Optional<LivingEntity> attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET);

        if (attackTarget.isPresent()) {
            if (!attackTarget.get().isAlive()) {
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            } else {
                double distanceSqr = entity.distanceToSqr(attackTarget.get());

                // 5マス未満 (25) -> 近接モードON
                if (distanceSqr < 25.0) {
                    if (!brain.hasMemoryValue(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get())) {
                        brain.setMemory(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get(), true);
                        LOGGER.info("Elite Skeleton is combat mode");
                        entity.setCombatStyle(false); // ★ここで直接「剣＋盾」に持ち替える
                    }
                }
                // 8マス以上離れた (64) -> 遠距離モード（近接モードOFF）
                else if (distanceSqr > 64.0) {
                    if (brain.hasMemoryValue(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get())) {
                        brain.eraseMemory(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get());
                        LOGGER.info("Elite Skeleton is ranged mode");
                        entity.setCombatStyle(true); // ★ここで直接「弓」に持ち替える
                    }
                }
            }
        }

        // ターゲット自動取得
        if (!brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
                    .ifPresent(player -> brain.setMemory(MemoryModuleType.ATTACK_TARGET, player));
        }
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
                MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
                MemoryModuleType.ATTACK_TARGET,
                ModBrains.SHIELD_DISABLED_COOLDOWN.get(),
                ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get()
        );
    }
}