package com.hiroto99.hardnight.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

public class CreeperPersistentTargetGoal extends TargetGoal {

    private LivingEntity target;
    private static final double MAX_DISTANCE = 32.0;

    public CreeperPersistentTargetGoal(Creeper creeper) {
        super(creeper, true);
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        if (!(t instanceof Player)) return false;
        if (!t.isAlive()) return false;

        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null) return false;
        if (!target.isAlive()) return false;

        // 視線は無視、距離のみチェック
        return mob.distanceToSqr(target) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    @Override
    public void start() {
        mob.setTarget(target);
    }

    @Override
    public void stop() {
        target = null;
        mob.setTarget(null);
    }
}