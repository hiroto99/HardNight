package com.hiroto99.hardnight.renderer;

import com.hiroto99.hardnight.entity.FrozenZombieHorse;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static com.hiroto99.hardnight.HardNight.MODID;

public class FrozenZombieHorseRenderer extends AbstractHorseRenderer<FrozenZombieHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {

    private final Identifier adultTexture = Identifier.fromNamespaceAndPath(MODID, "textures/entity/frozen_zombie_horse.png");
    private final Identifier babyTexture = Identifier.fromNamespaceAndPath(MODID, "textures/entity/frozen_zombie_horse_baby.png");

    public FrozenZombieHorseRenderer(EntityRendererProvider.Context context, EquipmentClientInfo.LayerType saddleLayer, ModelLayerLocation saddleModel) {
        super(context, new HorseModel(context.bakeLayer(ModelLayers.ZOMBIE_HORSE)), new BabyHorseModel(context.bakeLayer(ModelLayers.ZOMBIE_HORSE_BABY)));

        this.addLayer(new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_BODY,
                state -> state.bodyArmorItem,
                new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)),
                new HorseModel(context.bakeLayer(ModelLayers.HORSE))
        ));

        this.addLayer(new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                saddleLayer,
                state -> state.saddle,
                new EquineSaddleModel(context.bakeLayer(saddleModel)),
                new HorseModel(context.bakeLayer(ModelLayers.HORSE))
        ));
    }

    @Override
    public Identifier getTextureLocation(EquineRenderState state) {
        return state.isBaby ? this.babyTexture : this.adultTexture;
    }

    @Override
    public EquineRenderState createRenderState() {
        return new EquineRenderState();
    }
}

