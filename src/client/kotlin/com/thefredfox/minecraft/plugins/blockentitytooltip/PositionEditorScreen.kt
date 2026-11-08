package com.thefredfox.minecraft.plugins.blockentitytooltip

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

/**
 * Live position editor: pick a preset, type pixel offsets, or drag the sample box.
 *
 * Every change goes to [PositionDraft], so the HUD follows at once. Done keeps the draft for
 * Cloth's "Save & Quit"; Cancel and Escape restore the draft from when the editor opened.
 */
@Environment(EnvType.CLIENT)
class PositionEditorScreen(private val parent: Screen?) :
    Screen(Component.translatable("$I18N.editor.title")) {

    private val sampleText: Component = Component.literal("Diamond Block")
    private val padding = 4

    private var draggingBox = false
        set(value) {
            field = value
            children().filterIsInstance<AbstractWidget>().forEach {
                // EditBox ignores alpha, so the inputs hide instead of fading.
                if (it is EditBox) it.visible = !value else it.setAlpha(if (value) 0.2f else 1f)
            }
        }
    private var grabOffsetX = 0
    private var grabOffsetY = 0

    private val origAnchor = PositionDraft.anchor
    private val origOffsetX = PositionDraft.offsetX
    private val origOffsetY = PositionDraft.offsetY

    private val presetButtons = LinkedHashMap<PositionPreset, Button>()
    private lateinit var inputX: EditBox
    private lateinit var inputY: EditBox

    // Stops the EditBox responders from treating our own setValue calls as user input.
    private var syncingInputs = false

    private data class Box(val x: Int, val y: Int, val w: Int, val h: Int)

    private fun boxSize(): Pair<Int, Int> {
        val font = Minecraft.getInstance().font
        return (font.width(sampleText) + padding * 2) to (font.lineHeight + padding * 2)
    }

    private fun box(): Box {
        val (w, h) = boxSize()
        val (x, y) = positionFor(
            PositionDraft.anchor, PositionDraft.offsetX, PositionDraft.offsetY, width, height, w, h,
        )
        return Box(x, y, w, h)
    }

    private fun doneAndClose() {
        Minecraft.getInstance().setScreen(parent)
    }

    private fun revertAndClose() {
        PositionDraft.set(origAnchor, origOffsetX, origOffsetY)
        Minecraft.getInstance().setScreen(parent)
    }

    private fun selectPreset(preset: PositionPreset) {
        PositionDraft.set(preset)
        syncInputs()
    }

    private fun syncInputs() {
        syncingInputs = true
        inputX.value = PositionDraft.offsetX.toString()
        inputY.value = PositionDraft.offsetY.toString()
        syncingInputs = false
    }

    private fun onInputChanged() {
        if (syncingInputs) return
        val x = inputX.value.toIntOrNull() ?: return
        val y = inputY.value.toIntOrNull() ?: return
        PositionDraft.set(PositionDraft.anchor, x, y)
    }

    override fun init() {
        presetButtons.clear()
        // The center cell stays empty: the crosshair is there.
        val layout = listOf(
            Triple(PositionPreset.TOP_LEFT, 0, 0),
            Triple(PositionPreset.TOP_CENTER, 1, 0),
            Triple(PositionPreset.TOP_RIGHT, 2, 0),
            Triple(PositionPreset.MIDDLE_LEFT, 0, 1),
            Triple(PositionPreset.MIDDLE_RIGHT, 2, 1),
            Triple(PositionPreset.BOTTOM_LEFT, 0, 2),
            Triple(PositionPreset.ACTION_BAR, 1, 2),
            Triple(PositionPreset.BOTTOM_RIGHT, 2, 2),
        )
        val btnW = 96
        val btnH = 20
        val gap = 4
        val startX = (width - (3 * btnW + 2 * gap)) / 2
        val startY = 46
        for ((preset, col, row) in layout) {
            val button = Button.builder(Component.translatable(preset.key)) {
                selectPreset(preset)
            }.bounds(startX + col * (btnW + gap), startY + row * (btnH + gap), btnW, btnH).build()
            presetButtons[preset] = button
            addRenderableWidget(button)
        }

        val font = Minecraft.getInstance().font
        val inputsY = startY + 3 * (btnH + gap) + 2
        inputX = EditBox(font, width / 2 - 70, inputsY, 50, 18, Component.literal("X"))
        inputY = EditBox(font, width / 2 + 30, inputsY, 50, 18, Component.literal("Y"))
        listOf(inputX, inputY).forEach {
            it.setMaxLength(6)
            addRenderableWidget(it)
        }
        inputX.setResponder { onInputChanged() }
        inputY.setResponder { onInputChanged() }
        syncInputs()

        val bottomY = height - 28
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL) { revertAndClose() }
                .bounds(width / 2 - 154, bottomY, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.translatable("$I18N.editor.reset")) {
                selectPreset(PositionPreset.DEFAULT)
            }.bounds(width / 2 - 50, bottomY, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { doneAndClose() }
                .bounds(width / 2 + 54, bottomY, 100, 20).build()
        )
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val font = Minecraft.getInstance().font
        val white = 0xFFFFFFFF.toInt()
        val current = PositionDraft.preset

        // Same spot as vanilla's action-bar message (Gui draws it from H-72).
        val actionBar = Component.translatable("$I18N.editor.actionBarSample")
        graphics.text(font, actionBar, width / 2 - font.width(actionBar) / 2, height - 72, ARGB.color(0x80, 0xFF, 0xFF, 0xFF))

        presetButtons.forEach { (preset, button) -> button.active = current != preset }
        // Widgets derive hover from the mouse position; an off-screen one keeps those under the box unhovered.
        if (draggingBox || isOverBox(mouseX.toDouble(), mouseY.toDouble())) {
            super.extractRenderState(graphics, -1, -1, partialTick)
        } else {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        }

        // Drawn after the widgets: the box wins clicks over them, so it also draws on top.
        val b = box()
        graphics.fill(b.x, b.y, b.x + b.w, b.y + b.h, ARGB.color(0x88, 0x0, 0x0, 0x0))
        graphics.text(font, sampleText, b.x + padding, b.y + padding, white)

        graphics.text(font, title, width / 2 - font.width(title) / 2, 8, white)
        val hint = Component.translatable("$I18N.editor.hint")
        graphics.text(font, hint, width / 2 - font.width(hint) / 2, 20, white)
        val status = Component.translatable(
            "$I18N.editor.current",
            Component.translatable(current?.key ?: PositionPreset.CUSTOM_KEY),
            Component.translatable(PositionDraft.anchor.key),
        )
        graphics.text(font, status, width / 2 - font.width(status) / 2, 32, white)

        if (!draggingBox) {
            graphics.text(font, Component.literal("X"), inputX.x - 10, inputX.y + 5, white)
            graphics.text(font, Component.literal("Y"), inputY.x - 10, inputY.y + 5, white)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == 0 && isOverBox(event.x(), event.y())) {
            val b = box()
            draggingBox = true
            grabOffsetX = event.x().toInt() - b.x
            grabOffsetY = event.y().toInt() - b.y
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    private fun isOverBox(x: Double, y: Double): Boolean {
        val b = box()
        return x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (draggingBox) {
            val (w, h) = boxSize()
            val x = (event.x().toInt() - grabOffsetX).coerceIn(0, (width - w).coerceAtLeast(0))
            val y = (event.y().toInt() - grabOffsetY).coerceIn(0, (height - h).coerceAtLeast(0))
            val anchor = Anchor.nearest(x, y, w, h, width, height)
            val (ox, oy) = anchorOrigin(anchor, width, height, w, h)
            PositionDraft.set(anchor, x - ox, y - oy)
            syncInputs()
            return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (draggingBox && event.button() == 0) {
            draggingBox = false
            return true
        }
        return super.mouseReleased(event)
    }

    // Escape cancels; only the Done button keeps the edit.
    override fun onClose() {
        revertAndClose()
    }

    override fun isPauseScreen(): Boolean = false
}
