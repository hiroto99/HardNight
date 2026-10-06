package com.hiroto99.hardnight;

import com.hiroto99.windowslib.core.autodatagen.annotation.AutoTagforCustomTag;
import com.hiroto99.windowslib.instance.tagtype.TagTypeEntityType;
import com.hiroto99.windowslib.instance.tagtype.TagTypeItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import static com.hiroto99.hardnight.HardNight.MODID;

public class ModTags {
    @AutoTagforCustomTag(tagtype = TagTypeItem.class, elementValue = {"minecraft:brown_mushroom"})
    public static final TagKey<Item> FROZEN_ZOMBIE_HORSE_FOOD = bind(Registries.ITEM, "frozen_zombie_horse_food");
    @AutoTagforCustomTag(tagtype = TagTypeEntityType.class, elementValue = {"minecraft:chicken", "minecraft:cow", "minecraft:hoglin", "minecraft:mooshroom", "minecraft:pig", "minecraft:rabbit", "minecraft:sheep"})
    public static final TagKey<EntityType<?>> LIVE_STOCKS = bind(Registries.ENTITY_TYPE, "live_stocks");

    public static <T> TagKey<T> bind(ResourceKey<? extends Registry<T>> key, String location) {
        return TagKey.create(key, Identifier.fromNamespaceAndPath(MODID, location));
    }
}
