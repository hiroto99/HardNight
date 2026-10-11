package com.hiroto99.hardnight.entity;

import com.hiroto99.hardnight.register.ModBrains;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class EliteSkeleton extends Skeleton implements HoldingShieldMobBase {
    private String eliteBowPath = "inventory_bow";
    private String eliteSwordPath = "inventory_sword";

    // 保持用の ItemStack フィールド
    private ItemStack eliteBow = new ItemStack(Items.BOW);
    private ItemStack eliteSword = new ItemStack(Items.IRON_SWORD);

    public EliteSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // バニラ Skeleton の AI Goal を完全に無効化
    }

    protected Brain.Provider<EliteSkeleton> createBrainProvider() {
        return Brain.provider(
                List.of(
                        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                        MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
                        MemoryModuleType.WALK_TARGET,
                        MemoryModuleType.LOOK_TARGET,
                        MemoryModuleType.ATTACK_TARGET,
                        ModBrains.SHIELD_DISABLED_COOLDOWN.get(),
                        ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get()
                ),
                List.of(
                        SensorType.NEAREST_LIVING_ENTITIES,
                        SensorType.NEAREST_PLAYERS,
                        ModBrains.ELITE_SKELETON_SPECIFIC_SENSOR.get()
                ),
                (_) -> EliteSkeletonAI.getActivities()
        );
    }

    @Override
    protected Brain<EliteSkeleton> makeBrain(Brain.Packed packedBrain) {
        return this.createBrainProvider().makeBrain(this, packedBrain);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Brain<EliteSkeleton> getBrain() {
        return (Brain<EliteSkeleton>) super.getBrain();
    }

    // ★ エンチャントを保持したアイテムを切り替える
    public void setCombatStyle(boolean ranged) {
        ItemStack currentMainHand = this.getItemBySlot(EquipmentSlot.MAINHAND);

        if (ranged) {
            if (!ItemStack.isSameItem(currentMainHand, eliteBow)) {
                this.setItemSlot(EquipmentSlot.MAINHAND, eliteBow);
                this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                this.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
            }
        } else {
            if (!ItemStack.isSameItem(currentMainHand, eliteSword)) {
                this.setItemSlot(EquipmentSlot.MAINHAND, eliteSword);
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                this.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity livingEntity) {
            this.level().broadcastEntityEvent(this, (byte)4);
            EliteSkeletonAI.onHitTarget(this, livingEntity);
        } else {
            return false;
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean wasHurt = super.hurtServer(level, source, damage);
        if (wasHurt) {
            if (this.getBrain().hasMemoryValue(ModBrains.ELITE_SKELETON_IS_COMBAT_MODE.get()) && !this.getBrain().hasMemoryValue(ModBrains.SHIELD_DISABLED_COOLDOWN.get())) {
                if (canDisableShield(source)) {
                    this.getBrain().setMemory(ModBrains.SHIELD_DISABLED_COOLDOWN.get(), 100);
                }
                if (!canBlockable(source) && source.getEntity() instanceof LivingEntity sourceEntity) {
                    EliteSkeletonAI.wasHurtBy(level, this, sourceEntity);
                } else {
                    return false;
                }
            } else {
                if (source.getEntity() instanceof LivingEntity sourceEntity) {
                    EliteSkeletonAI.wasHurtBy(level, this, sourceEntity);
                }
            }
        }
        return wasHurt;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        ProfilerFiller profiler = Profiler.get();
        profiler.push("eliteSkeletonBrain");
        this.getBrain().tick(level, this);
        profiler.pop();

        EliteSkeletonAI.updateActivity(this);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (this.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
            var attr = this.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(50 + Math.random() * 70);
            }
        }
        if (this.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            var attr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attr != null) {
                attr.setBaseValue(2 + Math.random() * 3);
            }
        }
        if (this.getAttributes().hasAttribute(Attributes.ARMOR)) {
            var attr = this.getAttribute(Attributes.ARMOR);
            if (attr != null) {
                attr.setBaseValue(3 + Math.random() * 4);
            }
        }
        if (this.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE)) {
            var attr = this.getAttribute(Attributes.FOLLOW_RANGE);
            if (attr != null) {
                attr.setBaseValue(32 + Math.random() * 32);
            }
        }
        groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);

        // 1. 弓（eliteBow）にデフォルトの確率エンチャントを適用
        this.setItemSlot(EquipmentSlot.MAINHAND, eliteBow);
        this.populateDefaultEquipmentEnchantments(level, random, difficulty);
        // エンチャントがついた状態のものを eliteBow 変数に参照を保持
        // （JavaのItemStackは参照型なので、スロットに入れた状態でエンチャントメソッドを呼べばそのまま反映されます）

        // 2. 剣（eliteSword）を一度持たせてエンチャントを適用
        this.setItemSlot(EquipmentSlot.MAINHAND, eliteSword);
        this.populateDefaultEquipmentEnchantments(level, random, difficulty);

        // 3. 最終的に初期装備として弓をセットしてスタート
        this.setItemSlot(EquipmentSlot.MAINHAND, eliteBow);

        return groupData;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getStepSound() {
        return super.getStepSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return super.getDeathSound();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store(eliteBowPath, ItemStack.CODEC, eliteBow);
        output.store(eliteSwordPath, ItemStack.CODEC, eliteSword);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        eliteBow = input.read(eliteBowPath, ItemStack.CODEC).orElse(new ItemStack(Items.BOW));
        eliteSword = input.read(eliteSwordPath, ItemStack.CODEC).orElse(new ItemStack(Items.IRON_SWORD));
    }
}