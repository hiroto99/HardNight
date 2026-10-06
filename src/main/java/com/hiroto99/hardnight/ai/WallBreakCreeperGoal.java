package com.hiroto99.hardnight.ai;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class WallBreakCreeperGoal extends Goal {

    private final Creeper creeper;
    private final PathNavigation navigation;

    private Player target;
    private boolean priming;

    private int stuckTicks;
    private double lastDistance = Double.MAX_VALUE;

    private static final double SPEED = 1.1;
    private static final int BASE_STUCK_TICKS = 60;
    private static final double MAX_DETECT_RANGE = 24.0;
    private static final double NEAR_RANGE = 10.0;

    public WallBreakCreeperGoal(Creeper creeper) {
        this.creeper = creeper;
        this.navigation = creeper.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (creeper.getTarget() instanceof Player player && !player.isDeadOrDying()) {
            this.target = player;
            this.stuckTicks = 0;
            this.lastDistance = Double.MAX_VALUE;
            this.priming = false;
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        priming = false;
    }

    @Override
    public void tick() {
        LivingEntity currentTarget = creeper.getTarget();

        if (!(currentTarget instanceof Player) || currentTarget.isDeadOrDying()) {
            priming = false;
            return;
        }

        navigation.moveTo(currentTarget, SPEED);

        double dist = creeper.distanceTo(currentTarget);

        if (!priming) {
            if (dist >= lastDistance - 0.05) stuckTicks++;
            else stuckTicks = 0;

            lastDistance = dist;

            if (stuckTicks >= BASE_STUCK_TICKS && dist <= MAX_DETECT_RANGE) {
                if (dist <= NEAR_RANGE || stuckTicks >= BASE_STUCK_TICKS * 2) {
                    priming = true;
                    creeper.playSound(
                            SoundEvents.CREEPER_PRIMED,
                            1.0F,
                            1.0F
                    );
                }
            }
        }

        if (priming) {
            creeper.setSwellDir(1); // 表示だけ
        }
    }
}