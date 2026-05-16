package com.thefredfox.minecraft.plugins.blockentitytooltip

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

/**
 * The single live position hub. Pick a preset, type X/Y fractions, or drag the
 * box — all update [PositionDraft] and preview in the sample box.
 *
 * The HUD renders from the draft, so changes show in-game immediately.
 * Done keeps the edited draft (Cloth's "Save & Quit" then persists it; a
 * Cloth discard reverts it). Cancel / Escape roll the draft back to the
 * snapshot taken when the editor opened.
 */
@Environment(EnvType.CLIENT)
class PositionEditorScreen(private val parent: Screen?) :
    Screen(Component.literal("Tooltip Position")) {

    private val sampleText: Component = Component.literal("Diamond Block")
    private val padding = 4

    private var dragging = false
    private var grabOffsetX = 0.0
    private var grabOffsetY = 0.0

    // Snapshot of the draft as it was when the editor opened (for Cancel).
    private val origPreset = PositionDraft.preset
    private val origPosX = PositionDraft.posX
    private val origPosY = PositionDraft.posY

    private val presetButtons = LinkedHashMap<PositionPreset, Button>()
    private lateinit var inputX: EditBox
    private lateinit var inputY: EditBox

    /** Guards the EditBox responders while we set their text programmatically. */
    private var syncingInputs = false

    private fun round4(v: Double): Double = Math.round(v * 10000.0) / 10000.0

    /** The fraction actually in effect: custom → posX/posY, else the preset's. */
    private fun effectiveFraction(): Pair<Double, Double> =
        if (PositionDraft.preset == PositionPreset.CUSTOM) {
            PositionDraft.posX to PositionDraft.posY
        } else {
            PositionDraft.preset.fractionX to PositionDraft.preset.fractionY
        }

    private data class Box(val x: Int, val y: Int, val w: Int, val h: Int)

    private fun box(): Box {
        val font = Minecraft.getInstance().font
        val w = font.width(sampleText) + padding * 2
        val h = font.lineHeight + padding * 2
        val (px, py) = positionFor(
            PositionDraft.preset, PositionDraft.posX, PositionDraft.posY, width, height, w, h,
        )
        return Box(px, py, w, h)
    }

    /**
     * Keep the live-edited draft and return to the Cloth screen. The HUD
     * already reads the draft, so the in-game tooltip is at the new spot.
     * Cloth's "Save & Quit" persists it; a Cloth discard reverts it.
     */
    private fun doneAndClose() {
        Minecraft.getInstance().setScreen(parent)
    }

    /** Discard edits: restore the draft to the editor-open snapshot. */
    private fun revertAndClose() {
        PositionDraft.preset = origPreset
        PositionDraft.posX = origPosX
        PositionDraft.posY = origPosY
        Minecraft.getInstance().setScreen(parent)
    }

    private fun selectPreset(preset: PositionPreset) {
        PositionDraft.preset = preset
        PositionDraft.posX = round4(preset.fractionX)
        PositionDraft.posY = round4(preset.fractionY)
        syncInputs()
    }

    private fun syncInputs() {
        syncingInputs = true
        val (fx, fy) = effectiveFraction()
        inputX.value = round4(fx).toString()
        inputY.value = round4(fy).toString()
        syncingInputs = false
    }

    private fun onInputChanged() {
        if (syncingInputs) return
        val x = inputX.value.toDoubleOrNull() ?: return
        val y = inputY.value.toDoubleOrNull() ?: return
        PositionDraft.posX = round4(x.coerceIn(0.0, 1.0))
        PositionDraft.posY = round4(y.coerceIn(0.0, 1.0))
        PositionDraft.preset = PositionPreset.CUSTOM
    }

    override fun init() {
        presetButtons.clear()
        // 2 rows × 3 cols; ACTION_BAR sits where bottom-center would be
        // (bottom-center / center omitted — the hotbar is there).
        val layout = listOf(
            Triple(PositionPreset.TOP_LEFT, 0, 0),
            Triple(PositionPreset.TOP_CENTER, 1, 0),
            Triple(PositionPreset.TOP_RIGHT, 2, 0),
            Triple(PositionPreset.BOTTOM_LEFT, 0, 1),
            Triple(PositionPreset.ACTION_BAR, 1, 1),
            Triple(PositionPreset.BOTTOM_RIGHT, 2, 1),
        )
        val btnW = 96
        val btnH = 20
        val gap = 6
        val gridW = 3 * btnW + 2 * gap
        val startX = (width - gridW) / 2
        val startY = 60
        for ((preset, col, row) in layout) {
            val button = Button.builder(Component.translatable(preset.key)) {
                selectPreset(preset)
            }.bounds(startX + col * (btnW + gap), startY + row * (btnH + gap), btnW, btnH).build()
            presetButtons[preset] = button
            addRenderableWidget(button)
        }

        val font = Minecraft.getInstance().font
        val inputsY = startY + 2 * (btnH + gap) + 4
        inputX = EditBox(font, width / 2 - 70, inputsY, 50, 18, Component.literal("X"))
        inputY = EditBox(font, width / 2 + 30, inputsY, 50, 18, Component.literal("Y"))
        listOf(inputX, inputY).forEach {
            it.setMaxLength(8)
            addRenderableWidget(it)
        }
        inputX.setResponder { onInputChanged() }
        inputY.setResponder { onInputChanged() }
        syncInputs()

        val bottomY = height - 28
        addRenderableWidget(
            Button.builder(Component.literal("Cancel")) {
                revertAndClose()
            }.bounds(width / 2 - 154, bottomY, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Reset to default")) {
                selectPreset(PositionPreset.ACTION_BAR)
            }.bounds(width / 2 - 50, bottomY, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Done")) { doneAndClose() }
                .bounds(width / 2 + 54, bottomY, 100, 20).build()
        )
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        presetButtons.forEach { (preset, button) ->
            button.active = PositionDraft.preset != preset
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        val font = Minecraft.getInstance().font
        val white = 0xFFFFFFFF.toInt()

        val title = Component.literal("Tooltip Position")
        graphics.text(font, title, width / 2 - font.width(title) / 2, 14, white)
        val hint = Component.literal("Pick a preset, type X/Y (0–1), or drag the box.")
        graphics.text(font, hint, width / 2 - font.width(hint) / 2, 30, white)
        val current = Component.literal("Current: ")
            .append(Component.translatable(PositionDraft.preset.key))
        graphics.text(font, current, width / 2 - font.width(current) / 2, 44, white)

        graphics.text(font, Component.literal("X"), inputX.x - 10, inputX.y + 5, white)
        graphics.text(font, Component.literal("Y"), inputY.x - 10, inputY.y + 5, white)

        val b = box()
        graphics.fill(b.x, b.y, b.x + b.w, b.y + b.h, ARGB.color(0x88, 0x0, 0x0, 0x0))
        graphics.text(font, sampleText, b.x + padding, b.y + padding, white)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == 0) {
            val b = box()
            if (event.x() >= b.x && event.x() <= b.x + b.w &&
                event.y() >= b.y && event.y() <= b.y + b.h
            ) {
                dragging = true
                grabOffsetX = event.x() - b.x
                grabOffsetY = event.y() - b.y
                return true
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (dragging) {
            val b = box()
            val freeW = (width - b.w).toDouble()
            val freeH = (height - b.h).toDouble()
            val newX = (event.x() - grabOffsetX).coerceIn(0.0, maxOf(0.0, freeW))
            val newY = (event.y() - grabOffsetY).coerceIn(0.0, maxOf(0.0, freeH))
            PositionDraft.posX = round4(if (freeW <= 0.0) 0.0 else newX / freeW)
            PositionDraft.posY = round4(if (freeH <= 0.0) 0.0 else newY / freeH)
            PositionDraft.preset = PositionPreset.CUSTOM
            syncInputs()
            return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (dragging && event.button() == 0) {
            dragging = false
            return true
        }
        return super.mouseReleased(event)
    }

    // Escape / window close = Cancel (revert). Only the Done button commits.
    override fun onClose() {
        revertAndClose()
    }

    override fun isPauseScreen(): Boolean = false
}
