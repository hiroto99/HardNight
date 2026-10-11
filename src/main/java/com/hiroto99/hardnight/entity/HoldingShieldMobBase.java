package com.hiroto99.hardnight.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

public interface HoldingShieldMobBase {
    default boolean canDisableShield(DamageSource source) {
        if (source.getEntity() == null) return false;
        if (source.getEntity().is(EntityType.WARDEN) && source.typeHolder().is(DamageTypes.MOB_ATTACK)) return true;
        if (source.getEntity() instanceof LivingEntity living) {
            if (living.getMainHandItem().is(ItemTags.AXES)) return true;
        }
        if (source.getEntity() instanceof Player player && player.isCreative()) return true;
        return false;
    }

    default boolean canBlockable(DamageSource source) {
        if (source.typeHolder().is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            return arrow.getPierceLevel() == 0;
        }
        if (source.getEntity() instanceof Guardian && source.typeHolder().is(DamageTypes.MOB_ATTACK)) {
            return Math.random() <= 0.5;
        }
        if (source.getEntity() instanceof Player player && player.isCreative()) return true;
        return true;
    }
}
