package com.hiroto99.hardnight.client.model;

import com.hiroto99.hardnight.client.state.EliteSkeletonRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.util.Mth;

public class EliteSkeletonModel<T extends EliteSkeletonRenderState> extends SkeletonModel<T> {
    public EliteSkeletonModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(T state) {
        super.setupAnim(state);

        // オフハンドに盾を持っているか判定
        boolean hasShieldInOffhand = state.isHoldingShield;

        if (hasShieldInOffhand && !state.cantUseShield) {
            // 例: 盾を持っているときは、左腕（オフハンド）を前に突き出して構えるポーズにする
            // HumanoidModel の leftArm に対して回転角（xRot, yRot, zRot）を設定します

            if (state.isLeftHanded) {
                this.poseBlockingArm(this.rightArm, true);
            } else {
                this.poseBlockingArm(this.leftArm, false);
            }
        } else {
            if (state.isLeftHanded) {
                this.rightArm.yRot = 0.0F;
            } else {
                this.leftArm.yRot = 0.0F;
            }
        }
    }

    private void poseBlockingArm(ModelPart arm, boolean right) {
        // 腕の上下の回転 (xRot)
        // 0.9424779F (約54度) 持ち上げつつ、頭の向き (head.xRot) に連動させて上下を向かせる
        arm.xRot = arm.xRot * 0.5F - 0.9424779F + Mth.clamp(this.head.xRot, -1.3962634F, 0.43633232F);

        // 腕の左右の回転 (yRot)
        // 左右どちらの腕か (right) によって内側に抱え込むように角度を調整し、頭の向きに連動させる
        arm.yRot = (right ? -1F : 1F) + Mth.clamp(this.head.yRot, -0.5F, 0.5F);
    }
}
