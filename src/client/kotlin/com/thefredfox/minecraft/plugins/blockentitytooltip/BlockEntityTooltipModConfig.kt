package com.thefredfox.minecraft.plugins.blockentitytooltip

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

const val I18N = "text.autoconfig.block-entity-tooltip"

/**
 * A point on the 3×3 screen grid. The tooltip box keeps its pixel offset to this point,
 * so it stays put relative to the nearest edge across GUI scales and window sizes.
 */
enum class Anchor(val fractionX: Double, val fractionY: Double) {
    TOP_LEFT(0.0, 0.0),
    TOP_CENTER(0.5, 0.0),
    TOP_RIGHT(1.0, 0.0),
    MIDDLE_LEFT(0.0, 0.5),
    CENTER(0.5, 0.5),
    MIDDLE_RIGHT(1.0, 0.5),
    BOTTOM_LEFT(0.0, 1.0),
    BOTTOM_CENTER(0.5, 1.0),
    BOTTOM_RIGHT(1.0, 1.0);

    companion object {
        /** The anchor whose screen third holds the box center. */
        fun nearest(boxX: Int, boxY: Int, boxW: Int, boxH: Int, screenW: Int, screenH: Int): Anchor {
            val col = ((boxX + boxW / 2) * 3 / screenW.coerceAtLeast(1)).coerceIn(0, 2)
            val row = ((boxY + boxH / 2) * 3 / screenH.coerceAtLeast(1)).coerceIn(0, 2)
            return entries[row * 3 + col]
        }
    }
}

/** A named anchor and offset. Not stored: the editor derives it from the configured position. */
enum class PositionPreset(val anchor: Anchor, val offsetX: Int, val offsetY: Int) {
    TOP_LEFT(Anchor.TOP_LEFT, 4, 4),
    TOP_CENTER(Anchor.TOP_CENTER, 0, 4),
    TOP_RIGHT(Anchor.TOP_RIGHT, -4, 4),
    MIDDLE_LEFT(Anchor.MIDDLE_LEFT, 4, 0),
    MIDDLE_RIGHT(Anchor.MIDDLE_RIGHT, -4, 0),
    BOTTOM_LEFT(Anchor.BOTTOM_LEFT, 4, -4),

    // Vanilla draws action-bar messages from H-72 down; this keeps the box 2 px above them.
    ACTION_BAR(Anchor.BOTTOM_CENTER, 0, -74),
    BOTTOM_RIGHT(Anchor.BOTTOM_RIGHT, -4, -4);

    val key: String get() = "$I18N.positionPreset.$name"

    companion object {
        val DEFAULT = ACTION_BAR
        const val CUSTOM_KEY = "$I18N.positionPreset.CUSTOM"

        fun matching(anchor: Anchor, offsetX: Int, offsetY: Int): PositionPreset? =
            entries.firstOrNull { it.anchor == anchor && it.offsetX == offsetX && it.offsetY == offsetY }
    }
}

/** Marker for the synthetic "open position editor" entry; see [FREEHAND_GUI_PROVIDER]. */
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

    // Set only through PositionEditorScreen; excluded from the Cloth list but still serialized.
    @ConfigEntry.Gui.Excluded
    var anchor: Anchor = PositionPreset.DEFAULT.anchor,

    /** GUI pixels from [anchor], screen axes (+x right, +y down). */
    @ConfigEntry.Gui.Excluded
    var offsetX: Int = PositionPreset.DEFAULT.offsetX,

    @ConfigEntry.Gui.Excluded
    var offsetY: Int = PositionPreset.DEFAULT.offsetY,

    /** Value unused: the field only carries [FreehandEditorButton] into the Cloth list. */
    @FreehandEditorButton
    var openFreehandEditor: Boolean = false,
) : ConfigData {

    // Gson writes null for an unknown enum name and bypasses Kotlin's null checks.
    @Suppress("SENSELESS_COMPARISON")
    override fun validatePostLoad() {
        if (anchor == null) {
            anchor = PositionPreset.DEFAULT.anchor
            offsetX = PositionPreset.DEFAULT.offsetX
            offsetY = PositionPreset.DEFAULT.offsetY
        }
    }
}
