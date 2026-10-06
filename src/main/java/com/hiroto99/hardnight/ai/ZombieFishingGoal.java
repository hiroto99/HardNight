package com.hiroto99.hardnight.ai;

import com.hiroto99.hardnight.entity.ZombieFishingHookEntity;
import com.hiroto99.hardnight.register.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.EnumSet;

import static com.hiroto99.hardnight.HardNight.LOGGER;

public class ZombieFishingGoal extends MeleeAttackGoal {

    private final Zombie zombie;
    private int raiseArmTicks;
    private int cooldown = 0;
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 現在飛んでいるフック（1本制限） */
    private ZombieFishingHookEntity activeHook;

    public ZombieFishingGoal(Zombie zombie, double speedModifier, boolean trackTarget) {
        super(zombie, speedModifier, trackTarget);
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.TARGET));
        LOGGER.info("Goal added to zombie " + zombie.getUUID());
    }

    @Override
    public boolean canUse() {
        // 釣り竿を持っていなければ無効
        if (!zombie.getMainHandItem().is(Items.FISHING_ROD)) {
            return false;
        }

        // フックが既に存在するなら撃たない
        if (activeHook != null && activeHook.isAlive()) {
            return false;
        }

        LivingEntity target = zombie.getTarget();
        if (target == null) return false;

        double dist = zombie.distanceTo(target);
        return dist >= 5.0D
                && dist <= 40.0D
                && zombie.hasLineOfSight(target);
    }

    public void start() {
        super.start();
        this.raiseArmTicks = 0;
    }

    public void stop() {
        super.stop();
        this.zombie.setAggressive(false);
    }

    @Override
    public void tick() {
        super.tick();
        ++this.raiseArmTicks;
        if (this.raiseArmTicks >= 5 && this.getTicksUntilNextAttack() < this.getAttackInterval() / 2) {
            this.zombie.setAggressive(true);
        } else {
            this.zombie.setAggressive(false);
        }

        if (cooldown >= 0) {
            cooldown -= 1;
            return;
        }

        LivingEntity target = zombie.getTarget();
        if (target == null) return;

        if (!(zombie.level() instanceof ServerLevel level)) return;

        // フックがまだ生きていたら保険で中断
        if (activeHook != null && activeHook.isAlive()) {
            return;
        }

        cooldown = 100; // 約5秒

        activeHook = castLine(level, target);
    }

    private ZombieFishingHookEntity castLine(ServerLevel level, LivingEntity target) {
        ZombieFishingHookEntity hook = ModEntities.ZOMBIE_FISHING_HOOK.get().create(level, EntitySpawnReason.TRIGGERED);

        // ★ 必須：初期座標をゾンビに合わせる
        hook.setPos(
                zombie.getX(),
                zombie.getEyeY() - 0.1,
                zombie.getZ()
        );
        // ★ オーナー設定
        hook.setOwner(zombie);
        hook.setTargetEntity(target);

        double xd = target.getX() - zombie.getX();
        double yd = target.getEyeY() - (double)1.1F;
        double zd = target.getZ() - zombie.getZ();

        // 発射
        hook.shoot(
                xd, yd - hook.getY(), zd,
                0.8F,  // ★ ここが重要（初速）
                0.0F
        );
        level.addFreshEntity(hook);

        LOGGER.info("Hook spawned: " + hook.getId());
        return hook;
    }

}
