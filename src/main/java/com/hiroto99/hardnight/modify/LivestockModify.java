package com.hiroto99.hardnight.modify;

import com.hiroto99.hardnight.ai.HitAndAwayGoal;
import com.hiroto99.windowslib.ref.EntityRef;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.Optional;

import static com.hiroto99.hardnight.HardNight.LOGGER;
import static com.hiroto99.hardnight.ModTags.LIVE_STOCKS;

public class LivestockModify {
    private static final String PARENT_A_KEY = "ParentA_UUID";
    private static final String PARENT_B_KEY = "ParentB_UUID";

    // ==================================================
    // 0. Mod イベントバス（属性登録）
    // ==================================================
    @EventBusSubscriber
    public static class ModEvents {
        @SubscribeEvent
        public static void onAttributeModification(EntityAttributeModificationEvent event) {
            List<EntityType<? extends LivingEntity>> targets = List.of(
                    EntityType.COW,
                    EntityType.SHEEP,
                    EntityType.PIG,
                    EntityType.CHICKEN,
                    EntityType.MOOSHROOM,
                    EntityType.RABBIT
            );

            for (EntityType<? extends LivingEntity> type : targets) {
                // 各モブに応じたベース攻撃力を登録
                if (!event.has(type, Attributes.ATTACK_DAMAGE)) {
                    event.add(type, Attributes.ATTACK_DAMAGE, getBaseAttackDamage(type));
                }
                // ウシ・ムーシュルームにはノックバック属性を追加
                if (type.equals(EntityType.COW) || type.equals(EntityType.MOOSHROOM)) {
                    if (!event.has(type, Attributes.ATTACK_KNOCKBACK)) {
                        event.add(type, Attributes.ATTACK_KNOCKBACK, 1.5D); // 1.5に増強
                    }
                }
            }
        }
    }

    // ==================================================
    // 1. ゲーム内イベントバス
    // ==================================================
    @EventBusSubscriber
    public static class GameEvents {
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !shouldApplyThisModify(mob)) {
                return;
            }

            if (mob instanceof PathfinderMob pathfinderMob) {
                // 【重要】バニラのパニックAI（逃走挙動）を削除し、攻撃AIがキャンセルされないようにする
                mob.goalSelector.getAvailableGoals().removeIf(wrappedGoal ->
                        wrappedGoal.getGoal() instanceof PanicGoal
                );

                // 近接攻撃AIを付与
                double newSpeed = getSpeedModifier(mob);
                if (mob.is(EntityType.RABBIT)) {
                    mob.goalSelector.addGoal(1, new HitAndAwayGoal(pathfinderMob, newSpeed, newSpeed, 8.0F));
                } else {
                    mob.goalSelector.addGoal(1, new MeleeAttackGoal(pathfinderMob, newSpeed, true));
                }
            }

            // 2. 攻撃力属性値の設定
            if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                var attr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attr != null) {
                    double baseDamage = getBaseAttackDamage(mob.getType());
                    attr.setBaseValue(mob.isBaby() ? Math.max(1.0D, baseDamage / 2.0D) : baseDamage);
                }
            }

            // 3. ウシ・ムーシュルームのノックバック属性値を直接設定
            if (mob.getAttributes().hasAttribute(Attributes.ATTACK_KNOCKBACK)) {
                var knockbackAttr = mob.getAttribute(Attributes.ATTACK_KNOCKBACK);
                if (knockbackAttr != null) {
                    knockbackAttr.setBaseValue(1.0D);
                }
            }

            // 親登録処理（子供スポーン時）
            if (mob.isBaby()) {
                CompoundTag nbt = mob.getPersistentData();
                if (nbt.read(PARENT_A_KEY, UUIDUtil.CODEC).isEmpty()) {
                    AABB searchBox = mob.getBoundingBox().inflate(10.0D);
                    List<? extends Mob> nearbyMobs = mob.level().getEntitiesOfClass(mob.getClass(), searchBox, c -> !c.isBaby());

                    if (!nearbyMobs.isEmpty()) {
                        Mob parent = nearbyMobs.get(0);
                        nbt.store(PARENT_A_KEY, UUIDUtil.CODEC, parent.getUUID());
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onBabySpawn(BabyEntitySpawnEvent event) {
            if (event.getChild() instanceof Mob baby && shouldApplyThisModify(baby)) {
                CompoundTag nbt = baby.getPersistentData();

                if (event.getParentA() instanceof Mob parentA) {
                    nbt.store(PARENT_A_KEY, UUIDUtil.CODEC, parentA.getUUID());
                }
                if (event.getParentB() instanceof Mob parentB) {
                    nbt.store(PARENT_B_KEY, UUIDUtil.CODEC, parentB.getUUID());
                }
            }
        }

        // 被ダメージ時
        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent.Post event) {
            if (!(event.getEntity().level() instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof Mob victim) || !shouldApplyThisModify(victim)) {
                return;
            }
            if (victim.getHealth() <= event.getHealthDamage()) {
                return;
            }

            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                if (attacker == victim) {
                    return;
                }

                // 被弾した本人を敵対
                setAngerTarget(victim, attacker);

                // 子供が攻撃された場合、両親（ParentA / ParentB）のみ追加で敵対
                if (victim.isBaby()) {
                    CompoundTag nbt = victim.getPersistentData();

                    getRefFromNbt(nbt, PARENT_A_KEY).ifPresent(ref -> {
                        Mob parentA = ref.resolve(serverLevel);
                        if (parentA != null) setAngerTarget(parentA, attacker);
                    });
                    getRefFromNbt(nbt, PARENT_B_KEY).ifPresent(ref -> {
                        Mob parentB = ref.resolve(serverLevel);
                        if (parentB != null) setAngerTarget(parentB, attacker);
                    });
                }
                // パターンB: 被弾したのが「大人（親）」の場合 -> 自分 ＋ 周囲の自分の子供が反撃
                else {
                    AABB searchBox = victim.getBoundingBox().inflate(24.0D);
                    List<? extends Mob> nearbyBabies = victim.level().getEntitiesOfClass(victim.getClass(), searchBox, Mob::isBaby);

                    for (Mob baby : nearbyBabies) {
                        CompoundTag babyNbt = baby.getPersistentData();

                        Optional<EntityRef<Mob>> refA = getRefFromNbt(babyNbt, PARENT_A_KEY);
                        Optional<EntityRef<Mob>> refB = getRefFromNbt(babyNbt, PARENT_B_KEY);

                        boolean isChild = (refA.isPresent() && refA.get().uuid().equals(victim.getUUID())) ||
                                (refB.isPresent() && refB.get().uuid().equals(victim.getUUID()));

                        if (isChild) {
                            setAngerTarget(baby, attacker);
                        }
                    }
                }
            }
        }
    }

    // ==================================================
    // ヘルパーメソッド
    // ==================================================
    private static Optional<EntityRef<Mob>> getRefFromNbt(CompoundTag nbt, String key) {
        return nbt.read(key, UUIDUtil.CODEC).map(EntityRef::new);
    }

    private static void setAngerTarget(Mob mob, LivingEntity attacker) {
        if (mob == null || !mob.isAlive() || attacker == null) {
            return;
        }

        // 1. ターゲットおよびダメージ履歴の設定
        mob.setTarget(attacker);
        mob.setLastHurtByMob(attacker);
        if (attacker instanceof net.minecraft.world.entity.player.Player player) {
            mob.setLastHurtByPlayer(player, 100);
        }

        // 2. パニック行動を停止させ、攻撃AIを即座に起動する
        if (mob instanceof PathfinderMob pathfinderMob) {
            pathfinderMob.getNavigation().stop();
            pathfinderMob.getNavigation().moveTo(attacker, getSpeedModifier(pathfinderMob));

            // 追加された攻撃Goal（MeleeAttackGoal等）を探して直接起動する
            for (WrappedGoal wrappedGoal : mob.goalSelector.getAvailableGoals()) {
                if (wrappedGoal.getGoal() instanceof MeleeAttackGoal || wrappedGoal.getGoal() instanceof HitAndAwayGoal) {
                    if (wrappedGoal.canUse()) {
                        wrappedGoal.start();
                    }
                }
            }
        }
    }

    private static boolean shouldApplyThisModify(Mob mob) {
        if (mob instanceof Hoglin) {
            return false;
        } else {
            return mob.is(LIVE_STOCKS);
        }
    }

    private static double getBaseAttackDamage(@UnknownNullability EntityType<?> entity) {
        if (entity.equals(EntityType.RABBIT)) {
            return 5.0D;
        }
        if (entity.equals(EntityType.COW) || entity.equals(EntityType.MOOSHROOM) || entity.equals(EntityType.SHEEP)) {
            return 4.0D;
        }
        if (entity.equals(EntityType.PIG)) {
            return 3.5D;
        }
        if (entity.equals(EntityType.CHICKEN)) {
            return 2.0D;
        }
        return 0.0D;
    }

    private static double getSpeedModifier(Mob entity) {
        if (entity.is(EntityType.RABBIT)) {
            return 2.2D;
        }
        if (entity.is(EntityType.COW) || entity.is(EntityType.MOOSHROOM)) {
            return 2.0D;
        }
        if (entity.is(EntityType.CHICKEN)) {
            return 1.4D;
        }
        if (entity.is(EntityType.SHEEP)) {
            return 1.25D;
        }
        if (entity.is(EntityType.PIG)) {
            return 1.25D;
        }
        return 0.0D;
    }
}