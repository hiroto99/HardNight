package com.hiroto99.hardnight.register;

import com.hiroto99.hardnight.entity.EliteSkeletonSpecificSensor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

import static com.hiroto99.hardnight.HardNight.MODID;

public final class ModBrains {
    public static final DeferredRegister<SensorType<?>> SENSORS =
            DeferredRegister.create(Registries.SENSOR_TYPE, MODID);

    public static final DeferredHolder<SensorType<?>, SensorType<EliteSkeletonSpecificSensor>> ELITE_SKELETON_SPECIFIC_SENSOR =
            SENSORS.register("elite_skeleton_specific_sensor", () -> new SensorType<>(EliteSkeletonSpecificSensor::new));

    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULES =
            DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, MODID);

    // 盾破壊のクールダウン時間
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> SHIELD_DISABLED_COOLDOWN =
            MEMORY_MODULES.register("shield_disabled_cooldown",
                    () -> new MemoryModuleType<>(Optional.empty())); // Codecが不要な場合は Optional.empty()

    // EliteSkeleton: 近づかれたか
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> ELITE_SKELETON_IS_COMBAT_MODE =
            MEMORY_MODULES.register("elite_skeleton_is_combat_mode",
                    () -> new MemoryModuleType<>(Optional.empty())); // Codecが不要な場合は Optional.empty()

    public static final DeferredRegister<Activity> ACTIVITIES =
            DeferredRegister.create(Registries.ACTIVITY, MODID);

    // 「突撃」を表すカスタムActivity CHARGE
    public static final DeferredHolder<Activity, Activity> CHARGE =
            ACTIVITIES.register("charge", () -> new Activity("charge"));

    // 「遠距離攻撃」を表すカスタムActivity RANGED_ATTACK
    public static final DeferredHolder<Activity, Activity> RANGED_ATTACK =
            ACTIVITIES.register("ranged_attack", () -> new Activity("ranged_attack"));

    public static void init(IEventBus bus) {
        SENSORS.register(bus);
        MEMORY_MODULES.register(bus);
        ACTIVITIES.register(bus);
    }
}