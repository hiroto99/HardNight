package com.hiroto99.hardnight.entity;

import com.hiroto99.hardnight.ModTags;
import com.hiroto99.hardnight.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.DoubleSupplier;

public class FrozenZombieHorse extends AbstractHorse {
    private static final float SPEED_FACTOR = 42.16F;
    private static final double BASE_JUMP_STRENGTH = (double)0.5F;
    private static final double PER_RANDOM_JUMP_STRENGTH = 0.06666666666666667;
    private static final double BASE_SPEED = (double)9.0F;
    private static final double PER_RANDOM_SPEED = (double)1.0F;
    private static final EntityDimensions BABY_DIMENSIONS;

    public FrozenZombieHorse(EntityType<? extends FrozenZombieHorse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, (double)25.0F);
    }

    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        this.setPersistenceRequired();
        return super.interact(player, hand, location);
    }

    public boolean removeWhenFarAway(double distSqr) {
        return true;
    }

    public boolean isMobControlled() {
        return this.getFirstPassenger() instanceof Mob;
    }

    protected void randomizeAttributes(RandomSource random) {
        AttributeInstance var10000 = this.getAttribute(Attributes.JUMP_STRENGTH);
        Objects.requireNonNull(random);
        var10000.setBaseValue(generateZombieHorseJumpStrength(random::nextDouble));
        var10000 = this.getAttribute(Attributes.MOVEMENT_SPEED);
        Objects.requireNonNull(random);
        var10000.setBaseValue(generateZombieHorseSpeed(random::nextDouble));
    }

    private static double generateZombieHorseJumpStrength(DoubleSupplier probabilityProvider) {
        return (double)0.5F + probabilityProvider.getAsDouble() * 0.06666666666666667 + probabilityProvider.getAsDouble() * 0.06666666666666667 + probabilityProvider.getAsDouble() * 0.06666666666666667;
    }

    private static double generateZombieHorseSpeed(DoubleSupplier probabilityProvider) {
        return ((double)9.0F + probabilityProvider.getAsDouble() * (double)1.0F + probabilityProvider.getAsDouble() * (double)1.0F + probabilityProvider.getAsDouble() * (double)1.0F) / (double)42.16F;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // 氷渡りの能力
        if (level().isClientSide()) return;

        BlockPos pos = this.blockPosition();
        int radius = 2; // 氷化半径（Frost Walker Lv1 相当）

        for (BlockPos p : BlockPos.betweenClosed(
                pos.offset(-radius, -1, -radius),
                pos.offset(radius, -1, radius))) {

            BlockState state = level().getBlockState(p);

            if (state.is(Blocks.WATER)
                    && state.getFluidState().isSource()
                    && level().getBlockState(p.above()).isAir()) {

                BlockState blockState = Blocks.FROSTED_ICE.defaultBlockState();
                level().setBlock(p, blockState, 3);
                level().gameEvent(GameEvent.BLOCK_PLACE, p, GameEvent.Context.of(this, blockState));
            }
        }
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_HORSE_AMBIENT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_HORSE_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HORSE_HURT;
    }

    protected SoundEvent getAngrySound() {
        return SoundEvents.ZOMBIE_HORSE_ANGRY;
    }

    protected SoundEvent getEatingSound() {
        return SoundEvents.ZOMBIE_HORSE_EAT;
    }

    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return (AgeableMob)ModEntities.FROZEN_ZOMBIE_HORSE.get().create(level, EntitySpawnReason.BREEDING);
    }

    public boolean canFallInLove() {
        return false;
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, (double)1.25F, (i) -> i.is(ModTags.FROZEN_ZOMBIE_HORSE_FOOD), false));
    }

    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (spawnReason == EntitySpawnReason.NATURAL) {
            FrozenZombie zombie = (FrozenZombie) ModEntities.FROZEN_ZOMBIE.get().create(this.level(), EntitySpawnReason.JOCKEY);
            if (zombie != null) {
                zombie.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                zombie.finalizeSpawn(level, difficulty, spawnReason, (SpawnGroupData)null);
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
                zombie.startRiding(this, false, false);
            }
        }

        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean shouldOpenInventory = !this.isBaby() && this.isTamed() && player.isSecondaryUseActive();
        if (!this.isVehicle() && !shouldOpenInventory) {
            ItemStack itemStack = player.getItemInHand(hand);
            if (!itemStack.isEmpty()) {
                if (this.isFood(itemStack)) {
                    return this.fedFood(player, itemStack);
                }

                if (!this.isTamed()) {
                    this.makeMad();
                    return InteractionResult.SUCCESS;
                }
            }

            return super.mobInteract(player, hand);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    protected boolean handleEating(Player player, ItemStack itemStack) {
        boolean itemUsed = false;
        float heal = 0.0F;
        int ageUp = 0;
        int temper = 0;
        if (itemStack.is(Items.BROWN_MUSHROOM)) {
            heal = 3.0F;
            ageUp = 0;
            temper = 3;
        }

        if (this.getHealth() < this.getMaxHealth() && heal > 0.0F) {
            this.heal(heal);
            itemUsed = true;
        }

        if (this.isBaby() && ageUp > 0 && !this.isAgeLocked()) {
            this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX((double)1.0F), this.getRandomY() + (double)0.5F, this.getRandomZ((double)1.0F), (double)0.0F, (double)0.0F, (double)0.0F);
            if (!this.level().isClientSide()) {
                this.ageUp(ageUp);
                itemUsed = true;
            }
        }

        if (temper > 0 && (itemUsed || !this.isTamed()) && this.getTemper() < this.getMaxTemper() && !this.level().isClientSide()) {
            this.modifyTemper(temper);
            itemUsed = true;
        }

        if (itemUsed) {
            if (!this.isSilent()) {
                SoundEvent eatingSound = this.getEatingSound();
                if (eatingSound != null) {
                    this.level().playSound((Entity)null, this.getX(), this.getY(), this.getZ(), eatingSound, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
                }
            }

            this.gameEvent(GameEvent.EAT);
        }

        return itemUsed;
    }

    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    public boolean canBeLeashed() {
        return this.isTamed() || !this.isMobControlled();
    }

    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ModTags.FROZEN_ZOMBIE_HORSE_FOOD);
    }

    protected EquipmentSlot sunProtectionSlot() {
        return EquipmentSlot.BODY;
    }

    public Vec3[] getQuadLeashOffsets() {
        return Leashable.createQuadLeashOffsets(this, 0.04, 0.41, 0.18, 0.73);
    }

    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    public float chargeSpeedModifier() {
        return 1.4F;
    }

    public boolean canAgeUp() {
        return false;
    }

    static {
        BABY_DIMENSIONS = ModEntities.FROZEN_ZOMBIE_HORSE.get().getDimensions().withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, ModEntities.FROZEN_ZOMBIE_HORSE.get().getHeight() - 0.25F, 0.0F)).scale(0.7F);
    }
}
