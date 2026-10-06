package com.hiroto99.hardnight.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

import static com.hiroto99.hardnight.HardNight.MODID;

public class FrozenZombieRenderer extends ZombieRenderer {
    private static final Identifier BABY_LOCATION =
            Identifier.fromNamespaceAndPath(MODID, "textures/entity/frozen_zombie_baby.png");

    private static final Identifier LOCATION =
            Identifier.fromNamespaceAndPath(MODID, "textures/entity/frozen_zombie.png");

    public FrozenZombieRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState entity) {
        return entity.isBaby ? BABY_LOCATION : LOCATION;
    }
}
