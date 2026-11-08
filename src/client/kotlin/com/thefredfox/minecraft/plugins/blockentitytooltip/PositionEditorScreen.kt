package com.thefredfox.minecraft.plugins.blockentitytooltip

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

/**
 * Live position editor: pick a preset, type pixel offsets, or drag the sample box.
 *
 * Every change goes to [PositionDraft], so the HUD follows at once. Done keeps the draft for
 * Cloth's "Save & Quit"; Cancel and Escape restore the draft from when the editor opened.
 * The controls sit in one panel in the screen half away from the box, so the two never overlap.
 */
@Environment(EnvType.CLIENT)
class PositionEditorScreen(private val parent: Screen?) :
    Screen(Component.translatable("$I18N.editor.title")) {

    private val sampleText: Component = Component.literal("Diamond Block")
    private val padding = 4

    private var draggingBox = false
    private var grabOffsetX = 0
    private var grabOffsetY = 0

    private val origAnchor = PositionDraft.anchor
    private val origOffsetX = PositionDraft.offsetX
    private val origOffsetY = PositionDraft.offsetY

    private val presetButtons = LinkedHashMap<PositionPreset, Button>()
    private lateinit var inputX: EditBox
    private lateinit var inputY: EditBox
    private lateinit var anchorButton: CycleButton<Anchor>

    // Stops the EditBox responders from treating our own setValue calls as user input.
    private var syncingInputs = false

    /** Widgets with their position relative to the panel origin. */
    private val panelWidgets = mutableListOf<Triple<AbstractWidget, Int, Int>>()
    private var panelX = 0
    private var panelY = 0

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

    // Keeps the box in place: only the offsets change to the new anchor.
    private fun selectAnchor(anchor: Anchor) {
        val b = box()
        val (ox, oy) = anchorOrigin(anchor, width, height, b.w, b.h)
        PositionDraft.set(anchor, b.x - ox, b.y - oy)
        syncInputs()
    }

    /**
     * Marks the anchor on the screen and runs an L from it to the matching point on the box:
     * the horizontal leg is X, the vertical leg is Y.
     */
    private fun extractAnchorGuides(graphics: GuiGraphicsExtractor) {
        val anchor = PositionDraft.anchor
        val b = box()
        val ax = (anchor.fractionX * width).toInt().coerceIn(0, width - 1)
        val ay = (anchor.fractionY * height).toInt().coerceIn(0, height - 1)
        val px = (b.x + anchor.fractionX * b.w).toInt().coerceIn(b.x, b.x + b.w - 1)
        val py = (b.y + anchor.fractionY * b.h).toInt().coerceIn(b.y, b.y + b.h - 1)
        graphics.fill(minOf(ax, px), ay, maxOf(ax, px) + 1, ay + 1, GUIDE)
        graphics.fill(px, minOf(ay, py), px + 1, maxOf(ay, py) + 1, GUIDE)
        graphics.fill(ax - 2, ay - 2, ax + 3, ay + 3, GUIDE)
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

    /** Presets off the standard 4 px margin name their anchor in a separate tooltip key. */
    private fun tooltipKey(preset: PositionPreset): String =
        "${preset.key}.tooltip".takeIf { I18n.exists(it) } ?: preset.key

    private fun <T : AbstractWidget> place(widget: T, relX: Int, relY: Int): T {
        panelWidgets += Triple(widget, relX, relY)
        return addRenderableWidget(widget)
    }

    private fun panelWidth(): Int {
        val hintW = font.width(Component.translatable("$I18N.editor.hint"))
        return PAD * 2 + maxOf(GRID + COL_GAP + RIGHT_W, hintW)
    }

    /** Centers the panel in the screen half that does not hold the box center. */
    private fun layoutPanel() {
        val b = box()
        val half = height / 2
        val boxInUpperHalf = b.y + b.h / 2 < half
        panelX = (width - panelWidth()) / 2
        panelY = ((if (boxInUpperHalf) half else 0) + (half - PANEL_H) / 2).coerceAtLeast(0)
        panelWidgets.forEach { (w, rx, ry) -> w.setPosition(panelX + rx, panelY + ry) }
    }

    override fun init() {
        presetButtons.clear()
        panelWidgets.clear()

        val grid = listOf(
            Triple(PositionPreset.TOP_LEFT, 0, 0),
            Triple(PositionPreset.TOP_CENTER, 1, 0),
            Triple(PositionPreset.TOP_RIGHT, 2, 0),
            Triple(PositionPreset.MIDDLE_LEFT, 0, 1),
            Triple(PositionPreset.BELOW_CROSSHAIR, 1, 1),
            Triple(PositionPreset.MIDDLE_RIGHT, 2, 1),
            Triple(PositionPreset.BOTTOM_LEFT, 0, 2),
            Triple(PositionPreset.ACTION_BAR, 1, 2),
            Triple(PositionPreset.BOTTOM_RIGHT, 2, 2),
        )
        for ((preset, col, row) in grid) {
            val button = Button.builder(Component.literal(ARROWS.getValue(preset))) { selectPreset(preset) }
                .size(CELL, CELL)
                .tooltip(Tooltip.create(Component.translatable(tooltipKey(preset))))
                .build()
            presetButtons[preset] = place(button, PAD + col * (CELL + CELL_GAP), GRID_Y + row * (CELL + CELL_GAP))
        }

        val rx = PAD + GRID + COL_GAP
        anchorButton = place(
            CycleButton.builder<Anchor>({ Component.translatable(it.key) }, PositionDraft.anchor)
                .withValues(Anchor.entries)
                .create(0, 0, RIGHT_W, CELL, Component.translatable("$I18N.editor.anchor")) { _, a -> selectAnchor(a) },
            rx, GRID_Y,
        )
        inputX = place(EditBox(font, 0, 0, 50, 18, Component.literal("X")), rx + 10, GRID_Y + 22)
        inputY = place(EditBox(font, 0, 0, 50, 18, Component.literal("Y")), rx + 80, GRID_Y + 22)
        listOf(inputX, inputY).forEach { it.setMaxLength(6) }
        inputX.setResponder { onInputChanged() }
        inputY.setResponder { onInputChanged() }
        syncInputs()

        val actions = listOf(
            CommonComponents.GUI_CANCEL to { revertAndClose() },
            Component.translatable("$I18N.editor.reset") to { selectPreset(PositionPreset.DEFAULT) },
            CommonComponents.GUI_DONE to { doneAndClose() },
        )
        actions.forEachIndexed { i, (label, action) ->
            place(
                Button.builder(label) { action() }.size(BTN_W, CELL).build(),
                rx + i * (BTN_W + BTN_GAP), GRID_Y + GRID - CELL,
            )
        }
        layoutPanel()
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val white = 0xFFFFFFFF.toInt()
        val current = PositionDraft.preset
        layoutPanel()

        // Same spot as vanilla's action-bar message (Gui draws it from H-72).
        val actionBar = Component.translatable("$I18N.editor.actionBarSample")
        graphics.text(font, actionBar, width / 2 - font.width(actionBar) / 2, height - 72, ARGB.color(0x80, 0xFF, 0xFF, 0xFF))

        extractAnchorGuides(graphics)
        graphics.fill(panelX, panelY, panelX + panelWidth(), panelY + PANEL_H, ARGB.color(0xB0, 0x0, 0x0, 0x0))
        presetButtons.forEach { (preset, button) -> button.active = current != preset }
        if (anchorButton.value != PositionDraft.anchor) anchorButton.value = PositionDraft.anchor
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        // X/Y are relative to this anchor, also for a Custom position.
        presetButtons.entries.firstOrNull { it.key.anchor == PositionDraft.anchor }?.value?.let { cell ->
            val (x1, y1, x2, y2) = listOf(cell.x - 1, cell.y - 1, cell.x + cell.width + 1, cell.y + cell.height + 1)
            graphics.fill(x1, y1, x2, y1 + 1, white)
            graphics.fill(x1, y2 - 1, x2, y2, white)
            graphics.fill(x1, y1, x1 + 1, y2, white)
            graphics.fill(x2 - 1, y1, x2, y2, white)
        }

        graphics.text(font, Component.translatable("$I18N.editor.hint"), panelX + PAD, panelY + PAD, 0xFFA0A0A0.toInt())
        graphics.text(font, Component.literal("X"), inputX.x - 10, inputX.y + 5, white)
        graphics.text(font, Component.literal("Y"), inputY.x - 10, inputY.y + 5, white)

        val b = box()
        graphics.fill(b.x, b.y, b.x + b.w, b.y + b.h, ARGB.color(0x88, 0x0, 0x0, 0x0))
        graphics.text(font, sampleText, b.x + padding, b.y + padding, white)
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

    private companion object {
        const val PAD = 6
        const val CELL = 20
        const val CELL_GAP = 2
        const val GRID = 3 * CELL + 2 * CELL_GAP
        const val GRID_Y = PAD + 13
        const val COL_GAP = 8
        const val BTN_W = 62
        const val BTN_GAP = 5
        const val RIGHT_W = 3 * BTN_W + 2 * BTN_GAP
        const val PANEL_H = GRID_Y + GRID + PAD
        const val GUIDE = 0xFFFF4040.toInt()

        val ARROWS = mapOf(
            PositionPreset.TOP_LEFT to "↖",
            PositionPreset.TOP_CENTER to "↑",
            PositionPreset.TOP_RIGHT to "↗",
            PositionPreset.MIDDLE_LEFT to "←",
            PositionPreset.BELOW_CROSSHAIR to "·",
            PositionPreset.MIDDLE_RIGHT to "→",
            PositionPreset.BOTTOM_LEFT to "↙",
            PositionPreset.ACTION_BAR to "▣",
            PositionPreset.BOTTOM_RIGHT to "↘",
        )
    }
}
