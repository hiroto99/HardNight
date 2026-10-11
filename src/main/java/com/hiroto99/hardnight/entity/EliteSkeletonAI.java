package com.hiroto99.hardnight.entity;

import com.google.common.collect.ImmutableList;
import com.hiroto99.hardnight.entity.behavior.KitingRangedAttack;
import com.hiroto99.hardnight.modify.SkeletonNonHostileModify;
import com.hiroto99.hardnight.register.ModBrains;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.schedule.Activity;

import java.util.*;

public class EliteSkeletonAI {
    public EliteSkeletonAI() {
    }

    /**
     * すべての ActivityData を一括取得するエントリーポイント
     */
    protected static List<ActivityData<EliteSkeleton>> getActivities() {
        return List.of(
                initCoreActivity(),
                initIdleActivity(),
                initRangedFightActivity(),
                initDefaultFightActivity(),
                initChargeActivity()
        );
    }

    /**
     * 1. CORE（常に実行する基本挙動）
     * 首の動きや足の移動など、全状態共通の物理的な更新処理
     */
    private static ActivityData<EliteSkeleton> initCoreActivity() {
        return ActivityData.create(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new LookAtTargetSink(45, 90), // 首・視線をターゲットへ向ける
                        new MoveToTargetSink()         // WALK_TARGETへ足を進める
                )
        );
    }

    /**
     * 2. IDLE（非戦闘・通常時）
     * 平和な時の索敵とうろうろ移動
     */
    private static ActivityData<EliteSkeleton> initIdleActivity() {
        return ActivityData.create(
                Activity.IDLE,
                10,
                ImmutableList.of(
                        // たまにプレイヤーを眺める
                        SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
                        // ランダムに歩き回る / 立ち止まる
                        createIdleMovementBehaviors()
                )
        );
    }

    /**
     * 3. RANGED_ATTACK（通常戦闘：弓・遠距離攻撃）
     * 盾が健在（SHIELD_DISABLED_COOLDOWN が存在しない）時かつ8マス以上離れているときの標準戦闘
     */
    private static ActivityData<EliteSkeleton> initRangedFightActivity() {
        return ActivityData.create(
                ModBrains.RANGED_ATTACK.get(),
                6,
                ImmutableList.of(
                        StopAttackingIfTargetInvalid.create(),
                        // 引き撃ち Behavior: (射撃間隔20t, 最小距離12マス, 最大距離22マス)
                        new KitingRangedAttack(20, 12.0F, 22.0F)
                ),
                // この Activity の起動条件：ATTACK_TARGET の記憶が存在し、近接攻撃を行おうとしていないこと
                Set.of(
                        Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
                        Pair.of(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get(), MemoryStatus.VALUE_ABSENT)
                )
        );
    }

    /**
     * 4. FIGHT（通常戦闘：盾構え・近接攻撃）
     * 盾が健在（SHIELD_DISABLED_COOLDOWN が存在しない）時かつ5マス未満の標準戦闘
     */
    private static ActivityData<EliteSkeleton> initDefaultFightActivity() {
        return ActivityData.create(
                Activity.FIGHT,
                4,
                ImmutableList.of(
                        // 1. 無効ターゲットの解除
                        StopAttackingIfTargetInvalid.create(),

                        // 2. 視線合わせ（これがないとターゲットの方を向かず攻撃が出ないことがあります）
                        SetEntityLookTarget.create(target -> true, 16.0F),

                        // 3. ターゲットに接近（近接間隔に合わせて速度 1.2F で詰めさせる）
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.2F),

                        // 4. 近接攻撃（20ティック＝1秒周期）
                        MeleeAttack.create(20)
                ),
                // この Activity の起動条件：ATTACK_TARGET の記憶が存在し、近接攻撃を行おうとしていること
                Set.of(
                        Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
                        Pair.of(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get(), MemoryStatus.VALUE_PRESENT)
                )
        );
    }

    /**
     * 5. CHARGE（特殊戦闘：盾破壊時の高速突撃）
     * 斧で盾を破砕された（SHIELD_DISABLED_COOLDOWN が存在する）時の猛突撃
     */
    private static ActivityData<EliteSkeleton> initChargeActivity() {
        return ActivityData.create(
                ModBrains.CHARGE.get(),
                1,
                ImmutableList.of(
                        StopAttackingIfTargetInvalid.create(),
                        // 1.6F の高速移動でターゲットに猛猛接近
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.6F),
                        // 攻撃間隔も短縮（10ティック＝0.5秒周期で連撃）
                        MeleeAttack.create(10)
                ),
                // この Activity の起動条件：ATTACK_TARGET の記憶が存在し、近接攻撃を行おうとしていて、SHIELD_DISABLED_COOLDOWN の記憶が存在すること
                Set.of(
                        Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
                        Pair.of(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get(), MemoryStatus.VALUE_PRESENT),
                        Pair.of(ModBrains.SHIELD_DISABLED_COOLDOWN.get(), MemoryStatus.VALUE_PRESENT)
                )
        );
    }

    /**
     * 通常時のランダム移動の抽選
     */
    private static RunOne<EliteSkeleton> createIdleMovementBehaviors() {
        return new RunOne<>(
                ImmutableList.of(
                        Pair.of(RandomStroll.stroll(0.4F), 2),                // 確率2: うろうろ歩く
                        Pair.of(SetWalkTargetFromLookTarget.create(0.4F, 3), 2), // 確率2: 見ている方向へ歩く
                        Pair.of(new DoNothing(30, 60), 1)                      // 確率1: 立ち止まる
                )
        );
    }

    protected static void updateActivity(EliteSkeleton body) {
        Brain<EliteSkeleton> brain = body.getBrain();
        LivingEntity target = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);

        if (target != null) {
            double distanceSq = body.distanceToSqr(target);

            // 例: 6マス以内 (6 * 6 = 36) なら近接、それ以上なら遠距離へスタイル切り替え
            if (distanceSq <= 36.0) {
                if (!brain.hasMemoryValue(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get())) {
                    body.setCombatStyle(false); // 近接（剣＋盾）に切り替え
                }
            } else {
                if (brain.hasMemoryValue(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get())) {
                    body.setCombatStyle(true);  // 遠距離（弓）に切り替え
                }
            }
        }

        // 有効な Activity を優先順位に従ってセット
        brain.setActiveActivityToFirstValid(ImmutableList.of(
                ModBrains.CHARGE.get(),
                Activity.FIGHT,
                ModBrains.RANGED_ATTACK.get(),
                Activity.IDLE
        ));

        body.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }

    protected static void onHitTarget(EliteSkeleton attackerBody, LivingEntity target) {
        if (attackerBody != null && attackerBody.isAlive()) {
            broadcastAttackTarget(attackerBody, target);
        }
    }

    protected static void wasHurtBy(ServerLevel level, EliteSkeleton body, LivingEntity target) {
        if (body != null && body.isAlive()) {
            broadcastAttackTarget(body, target);

            double chance = 0.5 + (Math.random() / 4);
            if (target != null && body.getRandom().nextDouble() < chance && level.isSpawningMonsters()) {
                spawnSkeletonReinforcement(level, body, target);
            }
        }
    }

    private static void broadcastAttackTarget(EliteSkeleton body, LivingEntity target) {
        getVisibleSkeletons(body).forEach((skeleton) -> setAttackTargetIfCloserThanCurrent(skeleton, target));
    }

    private static void setAttackTargetIfCloserThanCurrent(AbstractSkeleton body, LivingEntity target) {
        body.setTarget(target);
        SkeletonNonHostileModify.addOrRewriteSkeletonUnion(body, target);
    }

    private static List<AbstractSkeleton> getVisibleSkeletons(EliteSkeleton body) {
        List<AbstractSkeleton> skeletons = body.level().getEntitiesOfClass(
                AbstractSkeleton.class, body.getBoundingBox().deflate(48)
        );
        return skeletons;
    }

    private static void spawnSkeletonReinforcement(ServerLevel level, EliteSkeleton body, LivingEntity target) {
        int x = Mth.floor(body.getX());
        int y = Mth.floor(body.getY());
        int z = Mth.floor(body.getZ());
        EntityType<Skeleton> type = EntityType.SKELETON;
        Skeleton reinforcement = type.create(level, EntitySpawnReason.REINFORCEMENT);
        if (reinforcement == null) {
            return;
        }

        for (int i = 0; i < 50; ++i) {
            RandomSource random = body.getRandom();
            int xt = x + Mth.nextInt(random, 7, 40) * Mth.nextInt(random, -1, 1);
            int yt = y + Mth.nextInt(random, 7, 40) * Mth.nextInt(random, -1, 1);
            int zt = z + Mth.nextInt(random, 7, 40) * Mth.nextInt(random, -1, 1);
            BlockPos spawnPos = new BlockPos(xt, yt, zt);
            if (SpawnPlacements.isSpawnPositionOk(type, level, spawnPos) && SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.REINFORCEMENT, spawnPos, level.getRandom())) {
                reinforcement.setPos((double)xt, (double)yt, (double)zt);
                if (!level.hasNearbyAlivePlayer((double)xt, (double)yt, (double)zt, (double)7.0F) && level.isUnobstructed(reinforcement) && level.noCollision(reinforcement) && !level.containsAnyLiquid(reinforcement.getBoundingBox())) {
                    setAttackTargetIfCloserThanCurrent(reinforcement, target);
                    reinforcement.finalizeSpawn(level, level.getCurrentDifficultyAt(reinforcement.blockPosition()), EntitySpawnReason.REINFORCEMENT, (SpawnGroupData)null);
                    level.addFreshEntityWithPassengers(reinforcement);
                    level.playSound(null, BlockPos.containing(reinforcement.position()), SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 1.5F, reinforcement.getVoicePitch() * 0.8F);
                    break;
                }
            }
        }
    }
}
