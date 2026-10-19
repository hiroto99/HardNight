package com.hiroto99.hardnight.client.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class ZombieFishingHookRenderState extends EntityRenderState {
    public Vec3 lineOffset = Vec3.ZERO;
    public boolean hooked;
    public float distance;
}
