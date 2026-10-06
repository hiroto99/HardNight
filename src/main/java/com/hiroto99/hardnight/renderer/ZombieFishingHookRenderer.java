package com.hiroto99.hardnight.renderer;

import com.hiroto99.hardnight.entity.ZombieFishingHookEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ZombieFishingHookRenderer
        extends EntityRenderer<ZombieFishingHookEntity, ZombieFishingHookRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutCull(TEXTURE_LOCATION);

    public ZombieFishingHookRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public boolean shouldRender(ZombieFishingHookEntity entity, Frustum culler, double camX, double camY, double camZ) {
        return super.shouldRender(entity, culler, camX, camY, camZ) && entity.getOwnerEntity() != null;
    }

    @Override
    public ZombieFishingHookRenderState createRenderState() {
        return new ZombieFishingHookRenderState();
    }

    @Override
    public void extractRenderState(
            ZombieFishingHookEntity entity,
            ZombieFishingHookRenderState state,
            float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);
        LivingEntity owner = entity.getOwnerEntity();
        if (owner == null) {
            state.lineOffset = Vec3.ZERO;
            return;
        }

        Vec3 from = owner.getEyePosition(partialTicks);
        Vec3 to = entity.getPosition(partialTicks);

        state.lineOffset = from.subtract(to);
        state.hooked = entity.isHooked();
        state.distance = (float) state.lineOffset.length();
    }

    @Override
    public void submit(
            ZombieFishingHookRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);

        // ---- 釣り竿描画（Vanilla式） ----
        collector.submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, buffer) -> {
                    vertex(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1);
                    vertex(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1);
                    vertex(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0);
                    vertex(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0);
                }
        );
        poseStack.popPose();

        Vec3 offset = state.lineOffset;
        float xa = (float) offset.x;
        float ya = (float) offset.y;
        float za = (float) offset.z;

        // ---- LOD（重要） ----
        int steps =
                state.distance > 15 ? 4 :
                        state.distance > 8  ? 6 : 8;

        // ---- カーブ描画（Vanilla式） ----
        float width = Minecraft.getInstance()
                .gameRenderer.getGameRenderState()
                .windowRenderState.appropriateLineWidth;

        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.lines(),
                (pose, buffer) -> {
                    for (int i = 0; i < steps; i++) {
                        float a0 = (float) i / steps;
                        float a1 = (float) (i + 1) / steps;
                        stringVertex(xa, ya, za, buffer, pose, a0, a1, width);
                        stringVertex(xa, ya, za, buffer, pose, a1, a0, width);
                    }
                }
        );
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int lightCoords, float x, float y, int u, int v) {
        buffer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    // Vanilla互換 stringVertex
    private static void stringVertex(
            float xa, float ya, float za,
            VertexConsumer buffer, PoseStack.Pose pose,
            float aa, float nexta, float width
    ) {
        float x = xa * aa;
        float y = ya * (aa * aa + aa) * 0.5F + 0.25F;
        float z = za * aa;

        float nx = xa * nexta - x;
        float ny = ya * (nexta * nexta + nexta) * 0.5F + 0.25F - y;
        float nz = za * nexta - z;

        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;

        buffer.addVertex(pose, x, y, z)
                .setColor(40, 40, 40, 255)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(width);
    }
}
