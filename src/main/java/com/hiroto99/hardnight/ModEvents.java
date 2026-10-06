package com.hiroto99.hardnight;

import com.hiroto99.hardnight.spawner.ZombieFishingSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class ModEvents {
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Zombie zombie
                && event.getLevel() instanceof ServerLevel) {
            ZombieFishingSpawner.onZombieSpawn(zombie);
        }
    }
}
