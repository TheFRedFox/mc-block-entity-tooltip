package com.thefredfox.minecraft.plugins.blockentitytooltip

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
 * A button-like AutoConfig entry. Cloth Config 26.1.x has no native button
 * list-entry, so this subclasses [TextListEntry] (which already implements the
 * full 26.1 entry contract).
 *
 * Cloth's element list dispatches `mouseClicked` to entries that do NOT
 * pre-filter by position (each entry must self-check bounds — the base
 * TextListEntry does this via its text hit-test). So we capture the rendered
 * row rectangle in [extractRenderState] and only act on a left click inside it;
 * otherwise the editor would open for clicks anywhere in the screen.
 */
@Suppress("DEPRECATION") // all TextListEntry constructors are @Deprecated; a builder can't give us a subclass
@Environment(EnvType.CLIENT)
class FreehandEditorEntry(label: Component) :
    TextListEntry(Component.literal("position_freehand"), label) {

    private var rowX = 0
    private var rowY = 0
    private var rowW = 0

    // The editor edits PositionDraft live (the HUD reads the draft, so Done
    // shows in-game immediately). Persistence is Cloth's: isEdited() enables
    // "Save & Quit" and the discard-confirm; save() (called by Cloth's
    // saveAll, never on discard) commits the draft into CONFIG so the
    // serializer writes it. Discard is inferred elsewhere via a tick check.
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
        if (event.button() == 0 &&
            event.x() >= rowX && event.x() <= rowX + rowW &&
            event.y() >= rowY && event.y() <= rowY + itemHeight
        ) {
            val mc = Minecraft.getInstance()
            mc.setScreen(PositionEditorScreen(mc.screen))
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }
}

/**
 * The position the HUD actually renders from. The editor mutates this live
 * (so Done shows in-game at once). It is committed into [CONFIG] only by
 * Cloth's "Save & Quit" ([FreehandEditorEntry.save]); a Cloth discard never
 * calls save, and a tick check then re-seeds the draft from [CONFIG] so the
 * in-game position rolls back too.
 */
object PositionDraft {
    var preset: PositionPreset = PositionPreset.ACTION_BAR
    var posX: Double = 0.5
    var posY: Double = 0.83

    fun loadFrom(c: BlockEntityTooltipModConfig) {
        preset = c.positionPreset
        posX = c.posX
        posY = c.posY
    }

    fun applyTo(c: BlockEntityTooltipModConfig) {
        c.positionPreset = preset
        c.posX = posX
        c.posY = posY
    }

    fun differsFrom(c: BlockEntityTooltipModConfig): Boolean =
        preset != c.positionPreset || posX != c.posX || posY != c.posY
}

/**
 * Registered for [FreehandEditorButton] in [BlockEntityTooltipClient]; replaces
 * the phantom marker field's default widget with the "open editor" entry.
 * Re-seeds [PositionDraft] from the committed config each time the screen builds.
 */
val FREEHAND_GUI_PROVIDER: GuiProvider = GuiProvider { _, _, _, _, _ ->
    PositionDraft.loadFrom(CONFIG)
    // Cloth 26.1 has no native button list-entry, so style the text to read
    // as an action: leading arrow + gold accent (Minecraft's usual highlight
    // colour). No underline — that read too much like an HTML hyperlink.
    val label = Component.literal("▶ ")
        .append(Component.translatable("text.autoconfig.block-entity-tooltip.option.openFreehandEditor"))
        .withStyle(ChatFormatting.GOLD)
    listOf<AbstractConfigListEntry<*>>(FreehandEditorEntry(label))
}
