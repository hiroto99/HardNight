package com.hiroto99.hardnight.modify;

import com.hiroto99.hardnight.entity.EliteSkeleton;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hiroto99.hardnight.HardNight.LOGGER;

@EventBusSubscriber
public class SkeletonNonHostileModify {
    private static final List<Pair<? extends AbstractSkeleton, LivingEntity>> SKELETON_UNIONS = new ArrayList<>();

    // スケルトンとターゲットのペアを追加または更新
    public static void addOrRewriteSkeletonUnion(AbstractSkeleton skeleton, LivingEntity target) {
        for (int i = 0; i < SKELETON_UNIONS.size(); i++) {
            if (SKELETON_UNIONS.get(i).getFirst() == skeleton) {
                // 既に存在する場合は上書きして処理を終了
                SKELETON_UNIONS.set(i, Pair.of(skeleton, target));
                return;
            }
        }
        // 存在しない場合のみ新規追加
        SKELETON_UNIONS.add(Pair.of(skeleton, target));
    }

    // ターゲットが死亡・無効化した際にリストから解除
    public static void resolveSkeletonUnionIf(LivingEntity target) {
        // 指定ターゲットに紐づくペアを取り除く
        SKELETON_UNIONS.removeIf(pair -> pair.getSecond().equals(target) || !pair.getFirst().isAlive());
    }

    // スケルトンがどの共通ターゲットに属しているか取得するヘルパー
    private static Optional<LivingEntity> getUnionTarget(AbstractSkeleton skeleton) {
        return SKELETON_UNIONS.stream()
                .filter(pair -> pair.getFirst() == skeleton && pair.getFirst().isAlive())
                .map(Pair::getSecond)
                .findFirst();
    }

    /**
     * ターゲット変更イベントをフックし、招集されたスケルトン同士の敵対を防止する
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        // ターゲットを変更しようとしているのが AbstractSkeleton か確認
        if (event.getEntity() instanceof AbstractSkeleton attacker) {
            LivingEntity newTarget = event.getNewAboutToBeSetTarget();

            // 1. 新しいターゲットも AbstractSkeleton の場合（スケルトン同士の同士討ち防止）
            if (newTarget instanceof AbstractSkeleton targetSkeleton) {
                Optional<LivingEntity> attackerUnionTarget = getUnionTarget(attacker);
                Optional<LivingEntity> targetUnionTarget = getUnionTarget(targetSkeleton);

                // 両者が招集リストに存在し、かつ同じ目標（プレイヤー等）を狙う仲間同士であればターゲット変更をキャンセル
                if (attackerUnionTarget.isPresent()
                        && targetUnionTarget.isPresent()
                        && attackerUnionTarget.get().equals(targetUnionTarget.get())) {
                    LOGGER.info("Not Changing Skeleton Target");
                    event.setCanceled(true);
                }
            }

            // 2. イベントがキャンセルされておらず、新しいターゲットが有効（生存している非スケルトンなど）な場合
            if (!event.isCanceled() && newTarget != null && newTarget.isAlive() && !(newTarget instanceof AbstractSkeleton)) {
                // EliteSkeleton が新たな敵（プレイヤー等）をターゲットした際、自動的に Union のリーダーとして登録/更新
                if (attacker instanceof EliteSkeleton eliteSkeleton) {
                    addOrRewriteSkeletonUnion(eliteSkeleton, newTarget);
                }
            }
        }
    }

    /**
     * ターゲットエンティティが死亡した時に、招集されたスケルトンのUnionを解除する
     * また招集されたスケルトンが死亡した場合も、そのスケルトンが属しているUnionから外す
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        resolveSkeletonUnionIf(event.getEntity());
    }
}