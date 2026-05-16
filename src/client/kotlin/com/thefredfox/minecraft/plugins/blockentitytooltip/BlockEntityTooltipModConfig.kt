package com.thefredfox.minecraft.plugins.blockentitytooltip

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry

private const val I18N = "text.autoconfig.block-entity-tooltip"

/**
 * Anchor presets. Non-special presets map to a normalized (fractionX, fractionY)
 * pair; the renderer places the box with `pos = (screenSize - boxSize) * fraction`
 * (0 = start edge, 1 = end edge, 0.5 = centered).
 *
 * [ACTION_BAR] is special-cased in the renderer (centered X, vanilla action-bar Y
 * band). [CUSTOM] uses [BlockEntityTooltipModConfig.posX]/posY from the freehand
 * editor.
 *
 * Implements [SelectionListEntry.Translatable] so the Cloth dropdown shows
 * translated labels instead of raw enum names.
 */
enum class PositionPreset(val fractionX: Double, val fractionY: Double) :
    SelectionListEntry.Translatable {
    TOP_LEFT(0.0, 0.0),
    TOP_CENTER(0.5, 0.0),
    TOP_RIGHT(1.0, 0.0),
    CENTER(0.5, 0.5),
    BOTTOM_LEFT(0.0, 1.0),
    BOTTOM_CENTER(0.5, 1.0),
    BOTTOM_RIGHT(1.0, 1.0),

    /** Just above the hotbar / where the vanilla action-bar text sits. */
    ACTION_BAR(0.5, 0.83),
    CUSTOM(1.0, 1.0);

    override fun getKey(): String = "$I18N.option.positionPreset.$name"
}

/** Marker for the synthetic "open freehand editor" entry; see PositionConfigGui. */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FreehandEditorButton

@Config(name = "block-entity-tooltip")
class BlockEntityTooltipModConfig(
    val enabled: Boolean = true,
    var distance: Double = 5.0,
    var showBlocks: Boolean = true,
    var showEntities: Boolean = true,
    var showFluids: Boolean = false,

    // Managed entirely by PositionEditorScreen, not editable Cloth fields.
    // @Gui.Excluded hides them from the config screen but they are still
    // serialized (Gson) and persisted.
    @ConfigEntry.Gui.Excluded
    var positionPreset: PositionPreset = PositionPreset.ACTION_BAR,

    /** Normalized box-position fraction (0..1), authoritative when [positionPreset] is [PositionPreset.CUSTOM]. */
    @ConfigEntry.Gui.Excluded
    var posX: Double = 0.5,

    @ConfigEntry.Gui.Excluded
    var posY: Double = 0.83,

    /**
     * Phantom field: value unused. Carries [FreehandEditorButton] so the
     * registered GuiProvider injects the position-editor launcher entry into
     * the main settings list. The editor is the single live position hub.
     */
    @FreehandEditorButton
    var openFreehandEditor: Boolean = false,
) : ConfigData
