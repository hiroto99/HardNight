package com.hiroto99.hardnight.spawner;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.hiroto99.hardnight.ai.ZombieFishingGoal;
import net.minecraft.world.level.Level;

public final class ZombieFishingSpawner {

    private ZombieFishingSpawner() {}

    public static void onZombieSpawn(Zombie zombie) {
        zombie.goalSelector.addGoal(
                2, // 攻撃咬みつきより高い優先度
                new ZombieFishingGoal(zombie, 1.0D, false)
        );

        if (!shouldHaveFishingRod(zombie)) {
            return;
        }

        zombie.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(Items.FISHING_ROD)
        );
    }

    private static boolean shouldHaveFishingRod(Zombie zombie) {
        float chance = calculateHaveFishingRodChance(zombie.level());
        return zombie.getRandom().nextFloat() <= chance;
    }

    private static float calculateHaveFishingRodChance(Level level) {
        // 基本難易度NORMALの時は2.5%、HARDの時は10%
        float chance = switch (level.getDifficulty()) {
            case NORMAL -> 0.025F;
            case HARD -> 0.10F;
            default -> 0.00F;
        };

        if (chance == 0.00F) {
            return 0.00F;
        }

        if (isDay(level)) {
            if (level.isRaining()) {
                // 難易度NORMALの時は1%、HARDの時は2.5%増加
                chance += level.getDifficulty() == Difficulty.NORMAL ? 0.01F : 0.025F;
            }
            if (level.isThundering()) {
                // 難易度NORMALの時は1%+1.5%で2.5%、難易度HARDの時は2.5%+5.0%で7.5%増加
                // 夜かつ雷雨の時は基本+夜雨ボーナス+雷雨ボーナス
                chance += level.getDifficulty() == Difficulty.NORMAL ? 0.025F : 0.075F;
            }
        }

        return chance;
    }

    private static boolean isDay(Level level) {
        return level.getOverworldClockTime() % 24000L < 12000L;
    }
}
