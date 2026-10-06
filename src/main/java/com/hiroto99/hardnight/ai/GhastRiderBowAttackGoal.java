package com.hiroto99.hardnight.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

public class GhastRiderBowAttackGoal<T extends Monster & RangedAttackMob> extends RangedBowAttackGoal<T> {

    private final T mob;
    private final float attackRadiusSqr;
    private final int attackIntervalMin;
    private int seeTime;
    private int attackTime = -1;

    public GhastRiderBowAttackGoal(T mob, double speedModifier, int attackIntervalMin, float attackRadius) {
        super(mob, speedModifier, attackIntervalMin, attackRadius);
        this.mob = mob;
        this.attackIntervalMin = attackIntervalMin;
        this.attackRadiusSqr = attackRadius * attackRadius;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        double targetDistSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
        boolean hadLineOfSight = this.seeTime > 0;

        if (hasLineOfSight != hadLineOfSight) {
            this.seeTime = 0;
        }

        if (hasLineOfSight) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        // --- ガスト乗車中はストレイフ（歩き回り）を無効化し、必殺の狙撃モードにする ---
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 弓の引き絞りと射撃処理
        if (this.mob.isUsingItem()) {
            if (!hasLineOfSight && this.seeTime < -60) {
                this.mob.stopUsingItem();
            } else if (hasLineOfSight) {
                int pullTime = this.mob.getTicksUsingItem();

                // 弓を引き絞ったら射撃（20tick = 1秒）
                if (pullTime >= 20) {
                    this.mob.stopUsingItem();

                    // ★ 改造ポイント：標準の performRangedAttack ではなく、100ブロック届く超高速射撃を実行
                    this.performCustomRangedAttack(target);

                    this.attackTime = this.attackIntervalMin;
                }
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, (item) -> item instanceof BowItem));
        }
    }

    /**
     * 100ブロック先までまっすぐ飛ばすカスタム射撃
     */
    private void performCustomRangedAttack(LivingEntity target) {
        ItemStack bowStack = this.mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof BowItem));
        ItemStack arrowStack = this.mob.getProjectile(bowStack);

        // 1. 矢エンティティの生成
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this.mob, arrowStack, 1.0F, bowStack);

        // 2. ターゲットの座標（プレイヤーの胸元）への相対ベクトルを計算
        double dx = target.getX() - this.mob.getX();
        // プレイヤーの体の中心（高さ）を狙う
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.mob.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // ★ 3. 100ブロック先まで失速させない初速設定（標準は1.6F〜3.0F程度）
        float velocity = 4.5F;

        // ★ 4. 精密狙撃（0.0Fに近づけるほどピンポイントで命中）
        float inaccuracy = 0.2F;

        // 放物線補正（距離に応じて若干上向きに補正）を加算して発射
        arrow.shoot(dx, dy + horizontalDistance * 0.025D, dz, velocity, inaccuracy);

        // ガスト本体への誤射を防ぐための識別タグ
        if (this.mob.getVehicle() != null) {
            arrow.addTag("ghast_rider_arrow");
        }

        this.mob.playSound(net.minecraft.sounds.SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        this.mob.level().addFreshEntity(arrow);
    }
}