package com.hiroto99.hardnight.entity.behavior;

import com.hiroto99.hardnight.entity.EliteSkeleton;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.Map;

public class KitingRangedAttack extends Behavior<EliteSkeleton> {

    private final int attackInterval;     // 射撃間隔（ティック）
    private final float minDistance;      // これより近づかれたら後退する距離
    private final float maxDistance;      // 射撃を行う最大距離
    private int attackCooldown = 0;       // 射撃タイマー

    public KitingRangedAttack(int attackInterval, float minDistance, float maxDistance) {
        super(Map.of(
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ));
        this.attackInterval = attackInterval;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    @Override
    protected void start(ServerLevel level, EliteSkeleton entity, long gameTime) {
        LivingEntity target = entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null) return;

        // ★ entity.setCombatStyle(true); を削除！

        // 視線を合わせる
        entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distanceSqr = entity.distanceToSqr(target);
        double minDistanceSqr = (double) minDistance * minDistance;
        double maxDistanceSqr = (double) maxDistance * maxDistance;

        // 移動制御
        if (distanceSqr < minDistanceSqr) {
            entity.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target.position(), 1.5F, (int) maxDistance)
            );
        } else if (distanceSqr > maxDistanceSqr) {
            entity.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, 2.0F, (int) minDistance)
            );
        } else {
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }

        // 射撃処理
        if (--this.attackCooldown <= 0 && distanceSqr <= maxDistanceSqr && entity.getSensing().hasLineOfSight(target)) {
            entity.performRangedAttack(target, 1.0F);
            this.attackCooldown = this.attackInterval;
        }
    }
}