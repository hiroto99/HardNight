package com.hiroto99.hardnight.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class HitAndAwayGoal extends Goal {
    protected final PathfinderMob mob;
    private final double approachSpeed;
    private final double retreatSpeed;
    private final float maxRetreatDistance; // 離脱を完了とみなす目標距離（例: 12.0F）
    private final float maxRetreatDistanceSqr; // 距離計算用の二乗値

    protected @Nullable Path retreatPath;
    private boolean isRetreating = false; // 現在離脱中かどうか

    public HitAndAwayGoal(PathfinderMob mob, double approachSpeed, double retreatSpeed, float maxRetreatDistance) {
        this.mob = mob;
        this.approachSpeed = approachSpeed;
        this.retreatSpeed = retreatSpeed;
        this.maxRetreatDistance = maxRetreatDistance;
        this.maxRetreatDistanceSqr = maxRetreatDistance * maxRetreatDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.isRetreating = false;
        this.retreatPath = null;
    }

    @Override
    public void stop() {
        this.isRetreating = false;
        this.retreatPath = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distanceSq = this.mob.distanceToSqr(target);

        // 1. 離脱モード中の処理
        if (this.isRetreating) {
            // 指定距離（例: 12ブロック）以上離れたら離脱モードを終了し、再び接近攻撃を開始する
            if (distanceSq >= this.maxRetreatDistanceSqr) {
                this.isRetreating = false;
                this.retreatPath = null;
                return;
            }

            // 現在の経路が終了（または未設定）の場合、さらに遠くへ逃げる経路を再計算
            if (this.mob.getNavigation().isDone() || this.retreatPath == null) {
                if (!this.tryStartRetreatPath(target)) {
                    // 逃げ道が見つからない（壁に追い詰められた等）場合は離脱を諦めて迎撃に切り替え
                    this.isRetreating = false;
                    return;
                }
                this.mob.getNavigation().moveTo(this.retreatPath, this.retreatSpeed);
            }
            return;
        }

        // 2. 接近・攻撃モード
        double attackReachSq = this.getAttackReachSqr(target);

        if (distanceSq <= attackReachSq) {
            // 攻撃実行
            if (this.mob.level() instanceof ServerLevel serverLevel) {
                this.mob.doHurtTarget(serverLevel, target);
            }

            // 攻撃ヒットと同時に「離脱モード」へ移行
            this.isRetreating = true;

            // AvoidEntityGoalのロジックで逃走ルートを設定
            if (this.tryStartRetreatPath(target)) {
                this.mob.getNavigation().moveTo(this.retreatPath, this.retreatSpeed);
            }
        } else {
            // 攻撃範囲外ならターゲットに接近
            this.mob.getNavigation().moveTo(target, this.approachSpeed);
        }
    }

    /**
     * AvoidEntityGoal 準拠の遠ざかる経路作成ロジック
     */
    private boolean tryStartRetreatPath(LivingEntity target) {
        // 16(横幅), 7(高低差) の範囲で遠ざかる位置を取得
        Vec3 pos = DefaultRandomPos.getPosAway(this.mob, 16, 7, target.position());
        if (pos == null) {
            return false;
        }

        // 取得位置が現在地よりターゲットから遠いことを確認
        if (target.distanceToSqr(pos.x, pos.y, pos.z) < target.distanceToSqr(this.mob)) {
            return false;
        }

        this.retreatPath = this.mob.getNavigation().createPath(pos.x, pos.y, pos.z, 0);
        return this.retreatPath != null;
    }

    private double getAttackReachSqr(LivingEntity target) {
        return (double) (this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + target.getBbWidth());
    }
}