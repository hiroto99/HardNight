package com.hiroto99.hardnight.register;

import com.hiroto99.hardnight.ModTags;
import com.hiroto99.hardnight.entity.FrozenZombie;
import com.hiroto99.hardnight.entity.FrozenZombieHorse;
import com.hiroto99.hardnight.entity.GhastRider;
import com.hiroto99.hardnight.entity.ZombieFishingHookEntity;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoLootTable;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoTag;
import com.hiroto99.windowslib.core.autodatagen.annotation.LootTablePool;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.LootType;
import com.hiroto99.windowslib.instance.tagtype.TagTypeEntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.hiroto99.hardnight.HardNight.MODID;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ZombieFishingHookEntity>> ZOMBIE_FISHING_HOOK =
            ENTITY_TYPES.register("zombie_fishing_hook", id ->
                    EntityType.Builder.of(
                                    ZombieFishingHookEntity::new,
                                    MobCategory.MISC
                            )
                            .noSave()      // フック系は必須
                            .noSummon()    // 推奨
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(32)   // ★ 必須
                            .updateInterval(1)         // ★ 推奨（Projectile系）
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))    // ★ ResourceKey<EntityType<?>> をそのまま渡す
            );

    @AutoTag(tagtype = TagTypeEntityType.class, tagKeyPath = {"minecraft:zombies", "minecraft:burn_in_daylight", "minecraft:powder_snow_walkable_mobs", "minecraft:freeze_immune_entity_types"})
    @AutoLootTable(
            name="frozen_zombie",
            type=LootType.ENTITY,
            pool={
                    @LootTablePool(
                            name="main",
                            rollsMin=1,
                            rollsMax=1,
                            dropItemData={"minecraft:rotten_flesh/1/0/2/1", "minecraft:ice/1/1/1/0/random_chance_with_looting_enchant/chance:0.11,per_level_above_first:0.02"}
                    ),
                    @LootTablePool(
                            name="zombie_horse_rider",
                            rollsMin=1,
                            rollsMax=1,
                            condition="entity_properties/entity:this,vehicle:hardnight:frozen_zombie_horse",
                            dropItemData={
                                    "minecraft:brown_mushroom/1/0/1/0"
                            }
                    ),
                    @LootTablePool(
                            name="iron_ingot",
                            rollsMin=1,
                            rollsMax=1,
                            dropItemData={
                                    "minecraft:iron_ingot/1/1/1/0/all_of[" +
                                            "random_chance_with_looting_enchant/chance:0.0083,per_level_above_first:0.0033;" +
                                            "entity_properties/entity:attacking_player]"
                            }
                    ),
                    @LootTablePool(
                            name="carrot",
                            rollsMin=1,
                            rollsMax=1,
                            dropItemData={
                                    "minecraft:carrot/1/1/1/0/all_of[" +
                                            "random_chance_with_looting_enchant/chance:0.0083,per_level_above_first:0.0033;" +
                                            "entity_properties/entity:attacking_player]"
                            }
                    ),
                    @LootTablePool(
                            name="potato",
                            rollsMin=1,
                            rollsMax=1,
                            dropItemData={
                                    "minecraft:potato/1/1/1/0/all_of[" +
                                            "random_chance_with_looting_enchant/chance:0.0083,per_level_above_first:0.0033;" +
                                            "entity_properties/entity:attacking_player]"
                            }
                    )
            }
    )
    public static final DeferredHolder<EntityType<?>, EntityType<FrozenZombie>> FROZEN_ZOMBIE =
            ENTITY_TYPES.register("frozen_zombie", id ->
                    EntityType.Builder.of(
                                    FrozenZombie::new,
                                    MobCategory.MONSTER
                            )
                            .sized(0.6f, 1.95f)
                            .eyeHeight(1.74F)
                            .passengerAttachments(2.0125F)
                            .ridingOffset(-0.7F)
                            .clientTrackingRange(8)
                            .notInPeaceful()
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))    // ★ ResourceKey<EntityType<?>> をそのまま渡す
            );

    @AutoTag(tagtype = TagTypeEntityType.class, tagKeyPath = {"minecraft:zombies", "minecraft:burn_in_daylight", "minecraft:powder_snow_walkable_mobs", "minecraft:freeze_immune_entity_types", "minecraft:can_equip_saddle", "minecraft:can_wear_horse_armor", "minecraft:can_float_while_ridden", "minecraft:cannot_be_age_locked"})
    @AutoLootTable(
            name="frozen_zombie_horse",
            type=LootType.ENTITY,
            pool={
                    @LootTablePool(
                            name="rotten_flesh",
                            rollsMin=1,
                            rollsMax=1,
                            dropItemData={
                                    "minecraft:rotten_flesh/1/2/3/1"
                            }
                    )
            }
    )
    public static final DeferredHolder<EntityType<?>, EntityType<FrozenZombieHorse>> FROZEN_ZOMBIE_HORSE =
            ENTITY_TYPES.register("frozen_zombie_horse", id ->
                    EntityType.Builder.of(
                                    FrozenZombieHorse::new,
                                    MobCategory.MONSTER
                            )
                            .sized(1.3964844F, 1.6F)
                            .eyeHeight(1.52F)
                            .passengerAttachments(1.31875F)
                            .clientTrackingRange(10)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))    // ★ ResourceKey<EntityType<?>> をそのまま渡す
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GhastRider>> GHAST =
            ENTITY_TYPES.register("ghast_rider", id ->
                    EntityType.Builder.of(
                                    GhastRider::new,
                                    MobCategory.MONSTER
                            )
                            .fireImmune()
                            .sized(4.0F, 4.0F)
                            .eyeHeight(2.6F)
                            .passengerAttachments(4.0625F)
                            .ridingOffset(0.5F)
                            .clientTrackingRange(10)
                            .notInPeaceful()
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );


    public static void init(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(FROZEN_ZOMBIE.get(), FrozenZombie.createAttributes().build());
        event.put(FROZEN_ZOMBIE_HORSE.get(), FrozenZombieHorse.createAttributes().build());
        event.put(GHAST.get(), Ghast.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(FROZEN_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.AND);
        event.register(FROZEN_ZOMBIE_HORSE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.AND);
        event.register(GHAST.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GhastRider::checkGhastRiderSpawnRules, RegisterSpawnPlacementsEvent.Operation.AND);
    }
}
