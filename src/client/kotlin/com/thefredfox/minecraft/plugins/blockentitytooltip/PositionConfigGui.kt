package com.thefredfox.minecraft.plugins.blockentitytooltip

import com.mojang.blaze3d.platform.InputConstants
import me.shedaniel.autoconfig.gui.registry.api.GuiProvider
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.gui.entries.TextListEntry
import net.minecraft.ChatFormatting
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Button-like Cloth entry that opens [PositionEditorScreen]; Cloth 26.1.x has no button entry.
 *
 * Cloth dispatches clicks to every entry, so the entry checks its own last rendered row bounds.
 */
@Suppress("DEPRECATION") // all TextListEntry constructors are @Deprecated; a builder can't give us a subclass
@Environment(EnvType.CLIENT)
class FreehandEditorEntry(label: Component) :
    TextListEntry(Component.literal("position_freehand"), label) {

    private var rowX = 0
    private var rowY = 0
    private var rowW = 0

    // Enables Cloth's "Save & Quit"; Cloth calls save() only on save, never on discard.
    override fun isEdited(): Boolean = PositionDraft.differsFrom(CONFIG)

    override fun save() {
        PositionDraft.applyTo(CONFIG)
        super.save()
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        index: Int,
        y: Int,
        x: Int,
        entryWidth: Int,
        entryHeight: Int,
        mouseX: Int,
        mouseY: Int,
        isHovered: Boolean,
        delta: Float,
    ) {
        rowX = x
        rowY = y
        rowW = entryWidth
        super.extractRenderState(
            graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta,
        )
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT &&
            event.x() >= rowX && event.x() <= rowX + rowW &&
            event.y() >= rowY && event.y() <= rowY + itemHeight
        ) {
            val mc = Minecraft.getInstance()
            mc.gui.setScreen(PositionEditorScreen(mc.gui.screen()))
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }
}

/**
 * The position the HUD renders from. The editor changes it live; [FreehandEditorEntry.save]
 * commits it to [CONFIG], and the client tick in [BlockEntityTooltipClient] re-seeds it after a discard.
 */
object PositionDraft {
    var anchor: Anchor = PositionPreset.DEFAULT.anchor
    var offsetX: Int = PositionPreset.DEFAULT.offsetX
    var offsetY: Int = PositionPreset.DEFAULT.offsetY

    val preset: PositionPreset? get() = PositionPreset.matching(anchor, offsetX, offsetY)

    fun set(anchor: Anchor, offsetX: Int, offsetY: Int) {
        this.anchor = anchor
        this.offsetX = offsetX
        this.offsetY = offsetY
    }

    fun set(preset: PositionPreset) = set(preset.anchor, preset.offsetX, preset.offsetY)

    fun loadFrom(c: BlockEntityTooltipModConfig) = set(c.anchor, c.offsetX, c.offsetY)

    fun applyTo(c: BlockEntityTooltipModConfig) {
        c.anchor = anchor
        c.offsetX = offsetX
        c.offsetY = offsetY
    }

    fun differsFrom(c: BlockEntityTooltipModConfig): Boolean =
        anchor != c.anchor || offsetX != c.offsetX || offsetY != c.offsetY
}

/** Replaces the [FreehandEditorButton] field's widget with the editor launcher. */
val FREEHAND_GUI_PROVIDER: GuiProvider = GuiProvider { _, _, _, _, _ ->
    PositionDraft.loadFrom(CONFIG)
    val label = Component.literal("▶ ")
        .append(Component.translatable("$I18N.option.openFreehandEditor"))
        .withStyle(ChatFormatting.GOLD)
    listOf<AbstractConfigListEntry<*>>(FreehandEditorEntry(label))
}
