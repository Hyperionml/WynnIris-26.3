package net.irisshaders.iris.compat.sodium.config;

import com.mojang.renderpearl.backend.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.*;
import net.caffeinemc.mods.sodium.client.config.builder.ColorThemeBuilderImpl;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatterImpls;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.gui.screen.AmbiencePackScreen;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.mixin.GpuDeviceAccessor;
import net.irisshaders.iris.mixin.IrisMixinPlugin;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PreferredGraphicsApi;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public class IrisConfig implements ConfigEntryPoint {
	public static final Identifier MONO = Identifier.fromNamespaceAndPath("iris", "textures/gui/config-icon-mono.png");
	public static final Identifier COLOR = Identifier.fromNamespaceAndPath("iris", "textures/gui/config-icon.png");
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        boolean vk = IrisMixinPlugin.usingVulkan;

        var modOptions = builder.registerOwnModOptions()
                .setName(Iris.MODNAME)
                .setIcon(MONO)
                .setColorTheme(builder.createColorTheme().setBaseThemeRGB(0xFFf556e2))
                .setVersion(Iris.getVersionSimple())
                .addPage(
                        builder.createExternalPage()
                                .setName(Component.translatable("options.iris.shaderPackSelection.title"))
                                .setScreenConsumer(i ->
                                        Minecraft.getInstance().gui.setScreen(vk ? new ShaderPackScreenPlaceholder(i) : new ShaderPackScreen(i)))
                );

        if (!vk) {
            // WynnIris: ambience pack selection screen
            modOptions.addPage(
                    builder.createExternalPage()
                            .setName(Component.translatable("options.iris.wynncraftAmbiencePackSelection.title"))
                            .setScreenConsumer(i ->
                                    Minecraft.getInstance().gui.setScreen(new AmbiencePackScreen(i)))
            );
            modOptions.addPage(createSettingsPage(builder));

            modOptions
                    .registerOptionOverlay(
                            Identifier.parse("sodium:quality.filtering_mode"),
                            builder.createEnumOption(
                                            Identifier.parse("sodium:quality.filtering_mode"),
                                            TextureFilteringMethod.class)
                                    .setTooltip(i -> {
                                        if (i == TextureFilteringMethod.RGSS) {
                                            return Component.translatable(
                                                            "options.textureFiltering." + i.name().toLowerCase(Locale.ROOT) + ".tooltip")
                                                    .append(Component.literal(" (RGSS is not usable with shaders on.)"));
                                        }

                                        return Component.translatable(
                                                "options.textureFiltering." + i.name().toLowerCase(Locale.ROOT) + ".tooltip");
                                    })
                                    .setAllowedValuesProvider(state -> {
                                        if (Iris.getCurrentPack().isPresent()) {
                                            return Set.of(
                                                    TextureFilteringMethod.NONE,
                                                    TextureFilteringMethod.ANISOTROPIC
                                            );
                                        }

                                        return Set.of(TextureFilteringMethod.values());
                                    }, ConfigState.UPDATE_ON_REBUILD)
                    )
                    .registerOptionOverlay(
                            Identifier.parse("sodium:quality.graphics"),
                            builder.createBooleanOption(
                                            Identifier.parse("sodium:quality.graphics"))
                                    .setTooltip(i -> {
                                        if (Iris.getCurrentPack().isPresent()) {
                                            return Component.literal(
                                                    "This option is not relevant when a shader pack is active.");
                                        }

                                        return Component.translatable(
                                                "options.improvedTransparency.tooltip");
                                    })
                                    .setEnabledProvider(
                                            i -> Iris.getCurrentPack().isEmpty(),
                                            ConfigState.UPDATE_ON_REBUILD
                                    )
                    )
                    .registerOptionOverlay(
                            Identifier.parse("sodium:general.graphics_api"),
                            builder.createEnumOption(Identifier.fromNamespaceAndPath("sodium", "general.graphics_api"),
                                            PreferredGraphicsApi.class)
                                    .setBinding((value) -> {
                                        if (((GpuDeviceAccessor) RenderSystem.getDevice()).getBackend() instanceof GlDevice && value == PreferredGraphicsApi.VULKAN) {
                                            Screen s = Minecraft.getInstance().gui.screen();

                                            Minecraft.getInstance().gui.setScreen(new ConfirmScreen(i -> {
                                                if (i) {
                                                    Minecraft.getInstance().options.preferredGraphicsBackend().set(value);
                                                    Minecraft.getInstance().gui.setScreen(s);
                                                } else {
                                                    s.onClose();
                                                    Minecraft.getInstance().gui.setScreen(VideoSettingsScreen.createScreen(Minecraft.getInstance().gui.screen()));
                                                }
                                            }, Component.literal("Iris"), Component.literal("Setting the backend to Vulkan will remove your ability to use shaders.\nWould you like to proceed?")));
                                        } else {
                                            Minecraft.getInstance().options.preferredGraphicsBackend().set(value);
                                        }
                                    }, () -> Minecraft.getInstance().options.preferredGraphicsBackend().get())
                    );
        }
    }

    private PageBuilder createSettingsPage(ConfigBuilder builder) {
        var page = builder.createOptionPage()
                .setName(Component.literal("Settings"))
                .addOptionGroup(
                        builder.createOptionGroup()
                                .addOption(
                                        builder.createExternalButtonOption(
                                                        Identifier.fromNamespaceAndPath("iris", "settings"))
                                                .setTooltip(Component.literal("Packs"))
                                                .setName(Component.translatable("options.iris.shaderPackList"))
                                                .setScreenConsumer(i ->
                                                        Minecraft.getInstance().gui.setScreen(new ShaderPackScreen(i)))
                                )
                )
                .addOptionGroup(
                        builder.createOptionGroup()
                                .addOption(createColorSpaceOption(builder))
                                .addOption(createShadowDistanceOption(builder))
                );

        // ---- WynnIris: glint / tint brightness ----
        page.addOptionGroup(
                builder.createOptionGroup()
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "glint_brightness"))
                                .setDefaultValue(110)
                                .setBinding(value -> IrisVideoSettings.glintBrightness = value, () -> IrisVideoSettings.glintBrightness)
                                .setName(Component.translatable("options.iris.glintBrightness"))
                                .setTooltip(Component.translatable("options.iris.glintBrightness.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(50, 200, 5))
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "tint_brightness"))
                                .setDefaultValue(75)
                                .setBinding(value -> IrisVideoSettings.tintBrightness = value, () -> IrisVideoSettings.tintBrightness)
                                .setName(Component.translatable("options.iris.tintBrightness"))
                                .setTooltip(Component.translatable("options.iris.tintBrightness.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(25, 150, 5))
                                .setImpact(OptionImpact.LOW)
                        )
        );

        // ---- WynnIris: text display brightness ----
        page.addOptionGroup(
                builder.createOptionGroup()
                        .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_text_brightness_floor"))
                                .setDefaultValue(false)
                                .setBinding(value -> IrisVideoSettings.wynncraftTextBrightnessFloor = value, () -> IrisVideoSettings.wynncraftTextBrightnessFloor)
                                .setName(Component.translatable("options.iris.wynncraftTextBrightnessFloor"))
                                .setTooltip(Component.translatable("options.iris.wynncraftTextBrightnessFloor.tooltip"))
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_text_brightness_floor_level"))
                                .setDefaultValue(10)
                                .setBinding(value -> IrisVideoSettings.wynncraftTextBrightnessFloorLevel = value, () -> IrisVideoSettings.wynncraftTextBrightnessFloorLevel)
                                .setName(Component.translatable("options.iris.wynncraftTextBrightnessFloorLevel"))
                                .setTooltip(Component.translatable("options.iris.wynncraftTextBrightnessFloorLevel.tooltip"))
                                .setValueFormatter(i -> Component.translatable("options.iris.wynncraftTextBrightnessFloorLevel.value", i))
                                .setEnabledProvider(i -> IrisVideoSettings.wynncraftTextBrightnessFloor, ConfigState.UPDATE_ON_REBUILD)
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(0, 15, 1))
                                .setImpact(OptionImpact.LOW)
                        )
        );

        // ---- WynnIris: skybox / entity lighting ----
        page.addOptionGroup(
                builder.createOptionGroup()
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_scene_darkening"))
                                .setDefaultValue(100)
                                .setBinding(value -> IrisVideoSettings.wynncraftSceneDarkening = value, () -> IrisVideoSettings.wynncraftSceneDarkening)
                                .setName(Component.translatable("options.iris.wynncraftSceneDarkening"))
                                .setTooltip(Component.translatable("options.iris.wynncraftSceneDarkening.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(0, 100, 5))
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_entity_brightness"))
                                .setDefaultValue(100)
                                .setBinding(value -> IrisVideoSettings.wynncraftEntityBrightness = value, () -> IrisVideoSettings.wynncraftEntityBrightness)
                                .setName(Component.translatable("options.iris.wynncraftEntityBrightness"))
                                .setTooltip(Component.translatable("options.iris.wynncraftEntityBrightness.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(0, 200, 5))
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_entity_emissivity"))
                                .setDefaultValue(100)
                                .setBinding(value -> IrisVideoSettings.wynncraftEntityEmissivity = value, () -> IrisVideoSettings.wynncraftEntityEmissivity)
                                .setName(Component.translatable("options.iris.wynncraftEntityEmissivity"))
                                .setTooltip(Component.translatable("options.iris.wynncraftEntityEmissivity.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(0, 100, 5))
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_night_vision_disables_boost"))
                                .setDefaultValue(true)
                                .setBinding(value -> IrisVideoSettings.wynncraftNightVisionDisablesBoost = value, () -> IrisVideoSettings.wynncraftNightVisionDisablesBoost)
                                .setName(Component.translatable("options.iris.wynncraftNightVisionDisablesBoost"))
                                .setTooltip(Component.translatable("options.iris.wynncraftNightVisionDisablesBoost.tooltip"))
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setImpact(OptionImpact.LOW)
                        )
        );

        // ---- WynnIris: Mist Woods biome fog ----
        page.addOptionGroup(
                builder.createOptionGroup()
                        .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_mist_woods_fog"))
                                .setDefaultValue(true)
                                .setBinding(value -> IrisVideoSettings.wynncraftMistWoodsFog = value, () -> IrisVideoSettings.wynncraftMistWoodsFog)
                                .setName(Component.translatable("options.iris.wynncraftMistWoodsFog"))
                                .setTooltip(Component.translatable("options.iris.wynncraftMistWoodsFog.tooltip"))
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_mist_woods_fog_density"))
                                .setDefaultValue(100)
                                .setBinding(value -> IrisVideoSettings.wynncraftMistWoodsFogDensity = value, () -> IrisVideoSettings.wynncraftMistWoodsFogDensity)
                                .setName(Component.translatable("options.iris.wynncraftMistWoodsFogDensity"))
                                .setTooltip(Component.translatable("options.iris.wynncraftMistWoodsFogDensity.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setEnabledProvider(i -> IrisVideoSettings.wynncraftMistWoodsFog, ConfigState.UPDATE_ON_REBUILD)
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(0, 100, 5))
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_mist_woods_fog_min_distance"))
                                .setDefaultValue(0)
                                .setBinding(value -> IrisVideoSettings.wynncraftMistWoodsFogMinDistance = value, () -> IrisVideoSettings.wynncraftMistWoodsFogMinDistance)
                                .setName(Component.translatable("options.iris.wynncraftMistWoodsFogMinDistance"))
                                .setTooltip(Component.translatable("options.iris.wynncraftMistWoodsFogMinDistance.tooltip"))
                                .setValueFormatter(i -> i == 0
                                        ? Component.translatable("options.iris.wynncraftMistWoodsFogMinDistance.default")
                                        : Component.translatable("options.iris.wynncraftMistWoodsFogMinDistance.blocks", i))
                                .setEnabledProvider(i -> IrisVideoSettings.wynncraftMistWoodsFog, ConfigState.UPDATE_ON_REBUILD)
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(0, 300, 10))
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_mist_woods_fog_sun_tint_reduction"))
                                .setDefaultValue(false)
                                .setBinding(value -> IrisVideoSettings.wynncraftMistWoodsFogSunTintReduction = value, () -> IrisVideoSettings.wynncraftMistWoodsFogSunTintReduction)
                                .setName(Component.translatable("options.iris.wynncraftMistWoodsFogSunTintReduction"))
                                .setTooltip(Component.translatable("options.iris.wynncraftMistWoodsFogSunTintReduction.tooltip"))
                                .setEnabledProvider(i -> IrisVideoSettings.wynncraftMistWoodsFog, ConfigState.UPDATE_ON_REBUILD)
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setImpact(OptionImpact.LOW)
                        )
                        .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_mist_woods_fog_sun_tint_amount"))
                                .setDefaultValue(50)
                                .setBinding(value -> IrisVideoSettings.wynncraftMistWoodsFogSunTintAmount = value, () -> IrisVideoSettings.wynncraftMistWoodsFogSunTintAmount)
                                .setName(Component.translatable("options.iris.wynncraftMistWoodsFogSunTintAmount"))
                                .setTooltip(Component.translatable("options.iris.wynncraftMistWoodsFogSunTintAmount.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setEnabledProvider(i -> IrisVideoSettings.wynncraftMistWoodsFog && IrisVideoSettings.wynncraftMistWoodsFogSunTintReduction, ConfigState.UPDATE_ON_REBUILD)
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setRange(new Range(0, 100, 5))
                                .setImpact(OptionImpact.LOW)
                        )
        );

        // ---- WynnIris: ambience (region-based shader profile switching) ----
        page.addOptionGroup(
                builder.createOptionGroup()
                        .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_ambience_enabled"))
                                .setDefaultValue(false)
                                .setBinding(value -> IrisVideoSettings.wynncraftAmbienceEnabled = value, () -> IrisVideoSettings.wynncraftAmbienceEnabled)
                                .setName(Component.translatable("options.iris.wynncraftAmbienceEnabled"))
                                .setTooltip(Component.translatable("options.iris.wynncraftAmbienceEnabled.tooltip"))
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setImpact(OptionImpact.MEDIUM)
                        )
                        .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_ambience_auto_warm_cache"))
                                .setDefaultValue(true)
                                .setBinding(value -> IrisVideoSettings.wynncraftAmbienceAutoWarmCache = value, () -> IrisVideoSettings.wynncraftAmbienceAutoWarmCache)
                                .setName(Component.translatable("options.iris.wynncraftAmbienceAutoWarmCache"))
                                .setTooltip(Component.translatable("options.iris.wynncraftAmbienceAutoWarmCache.tooltip"))
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setImpact(OptionImpact.HIGH)
                        )
        );

        // ---- WynnIris: mount armor overlay ----
        page.addOptionGroup(
                builder.createOptionGroup()
                        .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_mount_armor_overlay"))
                                .setDefaultValue(false)
                                .setBinding(value -> IrisVideoSettings.wynncraftMountArmorOverlay = value, () -> IrisVideoSettings.wynncraftMountArmorOverlay)
                                .setName(Component.translatable("options.iris.wynncraftMountArmorOverlay"))
                                .setTooltip(Component.translatable("options.iris.wynncraftMountArmorOverlay.tooltip"))
                                .setStorageHandler(IrisConfig::saveConfig)
                                .setImpact(OptionImpact.MEDIUM)
                        )
        );

        // ---- WynnIris: debug logging (experimental builds only) ----
        if (net.irisshaders.iris.BuildConfig.WYNNIRIS_EXPERIMENTAL) {
            page.addOptionGroup(
                    builder.createOptionGroup()
                            .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("iris", "wynncraft_debug_logging"))
                                    .setDefaultValue(false)
                                    .setBinding(value -> IrisVideoSettings.wynncraftDebugLogging = value, () -> IrisVideoSettings.wynncraftDebugLogging)
                                    .setName(Component.translatable("options.iris.wynncraftDebugLogging"))
                                    .setTooltip(Component.translatable("options.iris.wynncraftDebugLogging.tooltip"))
                                    .setStorageHandler(IrisConfig::saveConfig)
                                    .setImpact(OptionImpact.LOW)
                            )
            );
        }

        return page;
    }

    private OptionBuilder createColorSpaceOption(ConfigBuilder builder) {
        return builder.createEnumOption(
                        Identifier.fromNamespaceAndPath("iris", "color_space"),
                        ColorSpace.class)
                .setBinding(
                        value -> IrisVideoSettings.colorSpace = value,
                        () -> IrisVideoSettings.colorSpace
                )
                .setName(Component.translatable("options.iris.colorSpace"))
                .setDefaultValue(ColorSpace.SRGB)
                .setTooltip(Component.translatable("options.iris.colorSpace.sodium_tooltip"))
                .setStorageHandler(IrisConfig::saveConfig)
                .setElementNameProvider(ColorSpace::getName);
    }

    private OptionBuilder createShadowDistanceOption(ConfigBuilder builder) {
        return builder.createIntegerOption(
                        Identifier.fromNamespaceAndPath("iris", "shadow_distance"))
                .setDefaultValue(32)
                .setBinding(
                        value -> IrisVideoSettings.shadowDistance = value,
                        () -> IrisVideoSettings.getOverriddenShadowDistance(
                                IrisVideoSettings.shadowDistance)
                )
                .setName(Component.translatable("options.iris.shadowDistance"))
                .setTooltip(i -> {
                    if (!IrisVideoSettings.isShadowDistanceSliderEnabled()) {
                        return Component.translatable(
                                "options.iris.shadowDistance.disabled");
                    }

                    return Component.translatable(
                            "options.iris.shadowDistance.sodium_tooltip");
                })
                .setValueFormatter(
                        ControlValueFormatterImpls.quantityOrDisabled(
                                i -> Component.translatable("options.chunks", i),
                                Component.literal("None")
                        )
                )
                .setEnabledProvider(
                        i -> IrisVideoSettings.isShadowDistanceSliderEnabled(),
                        ConfigState.UPDATE_ON_REBUILD
                )
                .setStorageHandler(IrisConfig::saveConfig)
                .setRange(new Range(0, 32, 1))
                .setImpact(OptionImpact.HIGH);
    }

    private static void saveConfig() {
        try {
            Iris.getIrisConfig().save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
