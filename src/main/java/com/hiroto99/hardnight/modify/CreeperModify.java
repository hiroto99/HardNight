package com.hiroto99.hardnight.modify;

import com.hiroto99.hardnight.HardNight;
import com.hiroto99.hardnight.ai.CreeperPersistentTargetGoal;
import com.hiroto99.hardnight.ai.WallBreakCreeperGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import static com.hiroto99.hardnight.modify.CustomBabyMobModify.BABY_KEY;

@EventBusSubscriber(modid = HardNight.MODID)
public class CreeperModify {
    private static final String AIADDED_KEY  = "HardNightAIAdded";
    private static final String FUSE_KEY  = "HardNightFuse";

    private static final int BABY_FUSE_TICKS = 15; // ← 起爆までのtick（0.75秒）
    private static final float BABY_RADIUS  = 2.0F;

    @SubscribeEvent
    public static void onCreeperSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        var tag = creeper.getPersistentData();
        if (tag.getBoolean(AIADDED_KEY).orElse(false)) return;
        tag.putBoolean(AIADDED_KEY, true);

        creeper.goalSelector.addGoal(
                3,
                new WallBreakCreeperGoal(creeper)
        );

        creeper.targetSelector.addGoal(
                1,
                new CreeperPersistentTargetGoal(creeper)
        );
    }

    @SubscribeEvent
    public static void onCreeperTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!(creeper.level() instanceof ServerLevel level)) return;

        boolean blocked = isBlockedByWall(creeper);
        creeper.getPersistentData().putBoolean("HardNightWallBlocked", blocked);

        var tag = creeper.getPersistentData();
        if (!tag.getBoolean(BABY_KEY).orElse(false)) return;

        int fuse = tag.getInt(FUSE_KEY).orElse(0);
        // ✅ ターゲットがいないかクリエイティブ、距離が離れた、もしくはダメージ受けたなら減衰する
        if (creeper.getTarget() == null
                || creeper.distanceToSqr(creeper.getTarget()) > 9.0D
                || creeper.hurtTime > 0
                || (creeper.getTarget() instanceof Player p
                && (p.isCreative() || p.isSpectator()))
        ) {
            if (fuse > 0) {
                fuse--;
                tag.putInt(FUSE_KEY, fuse);
            }
            creeper.setSwellDir(0);
            return;
        }

        // 起爆開始
        fuse++;

        // 初回だけ起爆音
        if (fuse == 1) {
            creeper.playSound(
                    SoundEvents.CREEPER_PRIMED,
                    1F,
                    0.75F
            );
        }

        tag.putInt(FUSE_KEY, fuse);
        creeper.setSwellDir(1); // 見た目も起爆状態に

        // 自前爆発
        if (fuse >= BABY_FUSE_TICKS) {
            level.explode(
                    creeper,
                    creeper.getX(),
                    creeper.getY(),
                    creeper.getZ(),
                    BABY_RADIUS,
                    Level.ExplosionInteraction.MOB
            );
            creeper.discard(); // 本体削除
        }
    }

    private static boolean isBlockedByWall(Creeper creeper) {
        Vec3 forward = creeper.getLookAngle().normalize().scale(0.5);
        BlockPos pos = BlockPos.containing(
                creeper.getX() + forward.x,
                creeper.getY() + 0.5,
                creeper.getZ() + forward.z
        );
        return !creeper.level().getBlockState(pos).isAir();
    }
}
