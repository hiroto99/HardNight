package com.hiroto99.hardnight.entity;

import com.hiroto99.windowslib.util.Vec3Extender;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public class ZombieFishingHookEntity extends Projectile {

    private boolean hooked;
    private LivingEntity hookedEntity;
    private LivingEntity targetEntity;

    public ZombieFishingHookEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {}

    @Override
    public void tick() {
        /* LOGGER.info("Hook tick id=" + this.getId() +
                " hooked=" + hooked +
                " hookedEntity=" + hookedEntity +
                " owner=" + getOwner()); */
        super.tick();

        if (level().isClientSide()) return;

        // ★ フック前（飛翔中）
        if (!isHooked()) {
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (this.isAlive() && hitResult.getType() != HitResult.Type.MISS) {
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    onHitBlock((BlockHitResult) hitResult);
                }
                if (hitResult.getType() == HitResult.Type.ENTITY) {
                    onHitEntity((EntityHitResult) hitResult);
                }
            }

            this.move(MoverType.SELF, getDeltaMovement());

            // 重力
            this.applyGravity();

            // 空気抵抗
            setDeltaMovement(
                    getDeltaMovement().scale(0.98D)
            );

            return;
        }

        // ★ フック後
        LivingEntity target = getHookedEntity();
        LivingEntity owner  = getOwnerEntity();

        // ① 状態破綻
        if (target == null || owner == null || !target.isAlive()) {
            discard();
            return;
        }

        // ③ 引き切ったら終了
        if (this.distanceTo(target) < 1.5D) {
            discard();
            return;
        }

        // 引っ張り処理
        Vec3 dir = owner.position().subtract(target.position());
        if (dir.lengthSqr() < 1.0E-4) {
            discard();
            return;
        }
        double strength = 0.1D;

        target.setDeltaMovement(
                target.getDeltaMovement().add(dir.scale(strength))
        );

        target.hurtMarked = true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (isHooked()) return;
        super.onHitEntity(result);

        if (this.level().isClientSide()) return;

        Entity hit = result.getEntity();

        // 引っ張れる対象を限定（プレイヤー + Mob など）
        if (!(hit instanceof LivingEntity target)) return;

        LivingEntity owner = getOwnerEntity();
        if (owner == null) return;

        // 自分自身・同一チーム防止（任意）
        if (target == owner) return;

        ItemStack targetBlockingWith = target.getItemBlockingWith();
        BlocksAttacks blocksAttacks = targetBlockingWith != null ? targetBlockingWith.get(DataComponents.BLOCKS_ATTACKS) : null;
        if (blocksAttacks != null) {
            if ((this.level() instanceof ServerLevel serverLevel)) {
                blocksAttacks.disable(serverLevel, target, 5.0F, targetBlockingWith); // 斧と同じ時間だけ盾を使えなくする
                discard();
                return;
            }
        }

        if (target != getTargetEntity()) {
            if ((this.level() instanceof ServerLevel serverLevel)) {
                float ownerAttackDamage = (float) owner.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
                target.hurtServer(serverLevel, this.damageSources().mobProjectile(this, owner), ownerAttackDamage);
            }
        }

        // フック状態を確定
        this.setHookedEntity(target);
        this.setHooked(true);

        // 即時に「最初の引き」を入れる（体感が良くなる）
        pullOnce(target, this.getOwnerEntity().position());

        // フックをその場に固定
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) hitResult);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    private static void pullOnce(LivingEntity target, Vec3 ownerPos) {
        Vec3 delta = Vec3Extender.difference(ownerPos, target.position());

        if (delta.lengthSqr() < 0.0001) return;

        target.setDeltaMovement(
                target.getDeltaMovement().add(
                        delta.x,
                        delta.y,
                        delta.z
                )
        );

        target.hurtMarked = true; // クライアント同期
    }


    public void setHooked(boolean value) {
        this.hooked = value;
    }

    public boolean isHooked() {
        return this.hooked;
    }

    public void setHookedEntity(LivingEntity entity) {
        this.hookedEntity = entity;
    }

    public LivingEntity getHookedEntity() {
        return this.hookedEntity;
    }

    public void setTargetEntity(LivingEntity entity) {
        this.targetEntity = entity;
    }

    public LivingEntity getTargetEntity() {
        return this.targetEntity;
    }

    public LivingEntity getOwnerEntity() {
        Entity e = this.getOwner();
        return e instanceof LivingEntity le ? le : null;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwnerEntity();
        return entity instanceof LivingEntity
                && entity != owner
                && entity.isAlive();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.25F;
    }

    protected double getDefaultGravity() {
        return 0.03;
    }
}

