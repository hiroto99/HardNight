package com.hiroto99.hardnight.entity;

import com.hiroto99.hardnight.ai.GhastRiderBowAttackGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class GhastRider extends Ghast {

    public GhastRider(EntityType<? extends Ghast> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 既存のターゲットAIをクリアして100ブロック対応のプレイヤー索敵AIを追加
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null; // スケルトンではなくガスト自身が自律制御
    }

    // スポーン時のスケルトン4体の生成とAI強化
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        // 成功例と同じスタイルで記述（サーバー側かつ適切なスポーン条件）
        if (!level.isClientSide()) {
            for (int i = 0; i < 4; i++) {
                // JOCKEY（騎乗）としてスケルトンを生成
                Skeleton skeleton = EntityType.SKELETON.create(this.level(), EntitySpawnReason.JOCKEY);
                if (skeleton != null) {
                    // 1. 位置同期
                    skeleton.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);

                    // 2. スケルトンの100ブロック検知と攻撃AI設定
                    AttributeInstance followRange = skeleton.getAttribute(Attributes.FOLLOW_RANGE);
                    if (followRange != null) {
                        followRange.setBaseValue(100.0D);
                    }
                    skeleton.goalSelector.addGoal(2, new GhastRiderBowAttackGoal<>(skeleton, 1.0D, 20, 100.0F));
                    skeleton.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(skeleton, Player.class, true));

                    // 3. 初期化と装備
                    skeleton.finalizeSpawn(level, difficulty, spawnReason, null);
                    skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));

                    // 4. 搭乗（addFreshEntityを使わずに直接乗せる）
                    skeleton.startRiding(this, false, false);
                }
            }
        }

        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 4;
    }

    // 四隅配置（ガスト背上）
    @Override
    protected @NonNull Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = this.getPassengers().indexOf(passenger);

        double offsetX = 0.0;
        double offsetZ = 0.0;

        switch (index) {
            case 0 -> { offsetX =  1.7; offsetZ =  1.7; }
            case 1 -> { offsetX = -1.7; offsetZ =  1.7; }
            case 2 -> { offsetX =  1.7; offsetZ = -1.7; }
            case 3 -> { offsetX = -1.7; offsetZ = -1.7; }
            default -> { offsetX = 0.0; offsetZ = 0.0; }
        }

        double offsetY = (dimensions.height() * 0.75) + 1.2;
        Vec3 localOffset = new Vec3(offsetX, offsetY, offsetZ);
        return localOffset.yRot((float) -Math.toRadians(this.getYRot()));
    }

    public static boolean checkGhastRiderSpawnRules(EntityType<GhastRider> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && random.nextInt(20) == 0 && checkMobSpawnRules(type, level, spawnReason, pos, random);
    }
}
