package com.hiroto99.hardnight;

import com.hiroto99.hardnight.datagen.JapaneseProvider;
import com.hiroto99.hardnight.datagen.ModLanguageProvider;
import com.hiroto99.hardnight.register.ModEntities;
import com.hiroto99.hardnight.register.ModMobEffects;
import com.hiroto99.hardnight.renderer.FrozenZombieHorseRenderer;
import com.hiroto99.hardnight.renderer.FrozenZombieRenderer;
import com.hiroto99.hardnight.renderer.ZombieFishingHookRenderer;
import com.hiroto99.windowslib.datagen.AutoDataGenProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(HardNight.MODID)
public class HardNight {

	public static final String MODID = "hardnight";
	public static final Logger LOGGER = LogUtils.getLogger();

	public HardNight(IEventBus modEventBus, ModContainer modContainer) {
		// 共通登録
		ModEntities.init(modEventBus);
		ModMobEffects.init(modEventBus);

		AutoDataGenProvider.register("com.hiroto99.hardnight");

		NeoForge.EVENT_BUS.register(new ModEvents());

		// MODバスのイベント登録
		modEventBus.addListener(this::commonSetup);

		// ★ NoopRenderer 登録（描画しない）
		modEventBus.addListener(this::registerRenderers);

		modEventBus.addListener(ModEntities::createAttributes);
		modEventBus.addListener(ModEntities::registerSpawnPlacements);

		modEventBus.addListener(this::gatherData);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		LOGGER.info("HELLO FROM COMMON SETUP");
	}

	private void registerRenderers(
			EntityRenderersEvent.RegisterRenderers event
	) {
		event.registerEntityRenderer(
				ModEntities.ZOMBIE_FISHING_HOOK.get(),
                ZombieFishingHookRenderer::new
		);
		event.registerEntityRenderer(
				ModEntities.FROZEN_ZOMBIE.get(),
                FrozenZombieRenderer::new
		);
		event.registerEntityRenderer(
				ModEntities.FROZEN_ZOMBIE_HORSE.get(),
				context -> new FrozenZombieHorseRenderer(
						context,
						EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE,
						ModelLayers.ZOMBIE_HORSE_SADDLE
				)
		);
		event.registerEntityRenderer(
				ModEntities.GHAST.get(),
				GhastRenderer::new
		);
	}

	@SubscribeEvent
	private void gatherData(GatherDataEvent.Client event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();

		generator.addProvider(true, new ModLanguageProvider(output));
		generator.addProvider(true, new JapaneseProvider(output));
	}
}