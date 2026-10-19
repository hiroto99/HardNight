package com.hiroto99.hardnight.client.renderer;

import com.hiroto99.hardnight.client.model.EliteSkeletonModel;
import com.hiroto99.hardnight.client.model.ModModelLayers;
import com.hiroto99.hardnight.client.state.EliteSkeletonRenderState;
import com.hiroto99.hardnight.entity.EliteSkeleton;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public class EliteSkeletonRenderer extends AbstractSkeletonRenderer<EliteSkeleton, EliteSkeletonRenderState> {
    private static final Identifier ELITE_SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    public EliteSkeletonRenderer(EntityRendererProvider.Context context) {
        // 1. Boggedと同じように、親のコンストラクタに「コンテキスト」「アーマーレイヤー」「自作モデル」を渡す
        super(
                context,
                ModelLayers.SKELETON_ARMOR,
                new EliteSkeletonModel(context.bakeLayer(ModModelLayers.ELITE_SKELETON))
        );

        // 2. もしエリートスケルトン独自の外装や服（オーバーレイ層）を持たせたい場合は、
        //    Boggedの SkeletonClothingLayer のようにレイヤーを追加できます。
        // this.addLayer(new SkeletonClothingLayer<>(this, context.getModelSet(), ModModelLayers.ELITE_SKELETON_OUTER, ELITE_SKELETON_OUTER_LOCATION));
    }

    @Override
    public EliteSkeletonRenderState createRenderState() {
        return new EliteSkeletonRenderState();
    }

    @Override
    public void extractRenderState(EliteSkeleton entity, EliteSkeletonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        // 3. Boggedが isSheared を抽出しているのと同様に、
        //    キャストしてカスタムステートに「盾を持っているか」の情報を渡す
        state.isHoldingShield = entity.getOffhandItem().is(Items.SHIELD);
        state.cantUseShield = entity.isCantUseShield();
        state.isLeftHanded = entity.isLeftHanded();
    }

    @Override
    public Identifier getTextureLocation(EliteSkeletonRenderState state) {
        return ELITE_SKELETON_LOCATION;
    }
}