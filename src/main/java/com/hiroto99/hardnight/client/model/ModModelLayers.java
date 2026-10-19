package com.hiroto99.hardnight.client.model;

import com.hiroto99.hardnight.HardNight; // ご自身のMODのメインクラスやMOD IDに合わせて変更してください
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier; // あるいは ResourceLocation

public class ModModelLayers {
    // 登録するレイヤーのロケーションを定義
    // 第1引数: MOD ID, 第2引数: モデルの名前（パス）, 第3引数: パーツ名（通常は "main" など）
    public static final ModelLayerLocation ELITE_SKELETON = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(HardNight.MODID, "elite_skeleton"),
            "main"
    );
}
