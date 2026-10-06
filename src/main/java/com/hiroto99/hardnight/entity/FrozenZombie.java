package com.hiroto99.hardnight.entity;

import com.hiroto99.hardnight.register.ModMobEffects;
import net.minecraft.advancements.criterion.TagPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;

public class FrozenZombie extends Zombie {
    public FrozenZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, (double)35.0F).add(Attributes.MOVEMENT_SPEED, (double)0.23F).add(Attributes.ATTACK_DAMAGE, (double)3.0F).add(Attributes.ARMOR, (double)2.0F).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
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

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean result = super.doHurtTarget(level, target);

        if (result && target instanceof LivingEntity living) {
            living.addEffect(
                    new MobEffectInstance(
                            ModMobEffects.FREEZING,
                            200, // 10秒
                            0,   // Lv1（必要なら上げる）
                            false,
                            true,
                            true
                    )
            );
        }

        return result;
    }

    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BURN_FROM_STEPPING)){
            return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, damage);
        } else {
            return super.hurtServer(level, source, damage);
        }
    }
}
