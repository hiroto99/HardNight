package com.hiroto99.hardnight.modify;

import com.hiroto99.hardnight.HardNight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = HardNight.MODID)
public class CustomBabyMobModify {
    public static final String BABY_KEY = "IsBaby";

    // 確率（例：10%）
    public static final float BABY_CHANCE = 0.10f;

    public static final Identifier BABY_SCALE_ID = Identifier.fromNamespaceAndPath(HardNight.MODID, "baby_scale");
    public static final Identifier BABY_SPEED_ID = Identifier.fromNamespaceAndPath(HardNight.MODID, "baby_speed");

    @SubscribeEvent
    public static void onMobSpawn(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // 対象を Creeper / Spider に限定
        if (!(event.getEntity() instanceof Creeper
                || event.getEntity() instanceof Spider)) return;

        LivingEntity entity = (LivingEntity) event.getEntity();
        CompoundTag tag = entity.getPersistentData();

        // ① NBT に IsBaby が書いてあれば最優先
        if (tag.contains(BABY_KEY)) {
            return;
        }

        // ② なければ確率判定
        if (level.getRandom().nextFloat() < BABY_CHANCE) {
            tag.putBoolean(BABY_KEY, true);
        }
    }

    @SubscribeEvent
    public static void onMobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!(living instanceof Creeper || living instanceof Spider)) return;
        if (!(living.level() instanceof ServerLevel)) return;

        boolean baby = living.getPersistentData()
                .getBoolean(BABY_KEY)
                .orElse(false);

        var scaleAttr = living.getAttribute(Attributes.SCALE);
        var speedAttr = living.getAttribute(Attributes.MOVEMENT_SPEED);

        if (scaleAttr == null || speedAttr == null) {
            if (scaleAttr == null)
                HardNight.LOGGER.warn("Mob " + living.getType() + " has no SCALE attribute");
            if (speedAttr == null)
                HardNight.LOGGER.warn("Mob " + living.getType() + " has no SPEED attribute");

            return;
        }

        // 既存削除
        scaleAttr.removeModifier(BABY_SCALE_ID);
        speedAttr.removeModifier(BABY_SPEED_ID);

        if (baby) {
            scaleAttr.addTransientModifier(
                    new AttributeModifier(
                            BABY_SCALE_ID,
                            -0.5,  // 1.0 - 0.5 = 0.5
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );

            speedAttr.addTransientModifier(
                    new AttributeModifier(
                            BABY_SPEED_ID,
                            1, // +100%
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
        }
    }

    private static double getBabySpeed(LivingEntity e) {
        if (e instanceof Spider)  return 0.6D; // 壁登りも速くなる
        if (e instanceof Creeper) return 0.45D;
        return 0.25D;
    }

    private static double getNormalSpeed(LivingEntity e) {
        if (e instanceof Spider)  return 0.3D;
        if (e instanceof Creeper) return 0.25D;
        return 0.25D;
    }
}
