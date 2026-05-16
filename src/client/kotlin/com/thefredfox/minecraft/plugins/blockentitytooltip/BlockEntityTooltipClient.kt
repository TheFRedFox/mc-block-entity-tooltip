package com.thefredfox.minecraft.plugins.blockentitytooltip

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.AutoConfigClient
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen
import net.minecraft.client.gui.screens.ConfirmScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.slf4j.LoggerFactory

lateinit var CONFIG: BlockEntityTooltipModConfig

lateinit var CONFIG_HOLDER: ConfigHolder<BlockEntityTooltipModConfig>

object BlockEntityTooltipClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val LAYER_IDENTIFIER: Identifier = Identifier.fromNamespaceAndPath("block_entity_tooltip", "looking_at")
    private val lookingAtRenderer = LookingAtRenderer()

    override fun onInitializeClient() {
        AutoConfig.register(BlockEntityTooltipModConfig::class.java, ::GsonConfigSerializer)
        CONFIG_HOLDER = AutoConfig.getConfigHolder(BlockEntityTooltipModConfig::class.java)
        CONFIG_HOLDER.registerSaveListener { configHolder, config ->
            CONFIG = config
            InteractionResult.SUCCESS
        }
        CONFIG = CONFIG_HOLDER.config
        PositionDraft.loadFrom(CONFIG)
        AutoConfigClient.getGuiRegistry(BlockEntityTooltipModConfig::class.java)
            .registerAnnotationProvider(FREEHAND_GUI_PROVIDER, FreehandEditorButton::class.java)
        // Discard inference: Cloth gives no quit/discard hook. The draft is
        // legitimately diverged only while the config UI is in use — the Cloth
        // screen itself, our editor, or the discard-confirm dialog. The moment
        // we're on any other screen (parent screen after discard, or back in
        // game) with the draft still diverged, the user left without saving:
        // re-seed from config so the in-game position rolls back immediately.
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val s = client.screen
            val inConfigUi = s is AbstractConfigScreen ||
                s is PositionEditorScreen ||
                s is ConfirmScreen
            if (!inConfigUi && PositionDraft.differsFrom(CONFIG)) {
                PositionDraft.loadFrom(CONFIG)
            }
        }
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CROSSHAIR,
            LAYER_IDENTIFIER,
            lookingAtRenderer::extractRenderState
        )
    }
}

fun getNameOfLookedAt(player: Player, distance: Double = 5.0): String? {
    val hitResult = player.getLookingAt(distance)
        ?: return null

    when (hitResult.type) {
        HitResult.Type.BLOCK -> {
            if (CONFIG.showBlocks) {
                val cHitResult = hitResult as BlockHitResult
                val blockPos = cHitResult.blockPos
                val blockState = player.level().getBlockState(blockPos)
                val block = blockState.block
                return block.name.string
            } else {
                return null
            }
        }

        HitResult.Type.ENTITY -> {
            if (CONFIG.showEntities) {
                val cHitResult = hitResult as EntityHitResult
                val entity = cHitResult.entity
                return entity.name.string
            } else {
                return null
            }
        }

        HitResult.Type.MISS -> return null
        null -> return null
    }

}

class LookingAtRenderer : HudElement {
    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        if (!CONFIG.enabled) {
            return
        }

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return
        val world = client.level
            ?: return

        getNameOfLookedAt(player, CONFIG.distance)?.let { text ->
            val font = client.font
            val textObj = Component.literal(text)

            val screenWidth = graphics.guiWidth()
            val screenHeight = graphics.guiHeight()

            val padding = 4

            val textWidth = font.width(textObj)
            val textHeight = font.lineHeight
            val boxW = textWidth + padding * 2
            val boxH = textHeight + padding * 2

            val (boxX, boxY) = positionFor(
                PositionDraft.preset, PositionDraft.posX, PositionDraft.posY,
                screenWidth, screenHeight, boxW, boxH,
            )

            val bgColor = ARGB.color(0x88, 0x0, 0x0, 0x0)
            val textColor = 0xFFFFFFFF.toInt()

            graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, bgColor)
            graphics.text(font, textObj, boxX + padding, boxY + padding, textColor)
        }
    }
}

/**
 * Maps the configured position to top-left pixel coordinates of the tooltip box.
 *
 * Presets and the freehand editor share this single formula so "what you drag is
 * what you get". Position is expressed as a fraction of the free space
 * (`screen - box`), making it independent of GUI scale and resolution.
 * [PositionPreset.ACTION_BAR] is just the preset fraction (0.5, 0.83) — the
 * spot just above the hotbar that the user dialed in.
 */
fun positionFor(
    preset: PositionPreset,
    posX: Double,
    posY: Double,
    screenW: Int,
    screenH: Int,
    boxW: Int,
    boxH: Int,
): Pair<Int, Int> {
    val freeW = (screenW - boxW).coerceAtLeast(0)
    val freeH = (screenH - boxH).coerceAtLeast(0)
    return when (preset) {
        PositionPreset.CUSTOM -> {
            val x = (freeW * posX).toInt().coerceIn(0, freeW)
            val y = (freeH * posY).toInt().coerceIn(0, freeH)
            x to y
        }

        else -> (freeW * preset.fractionX).toInt() to (freeH * preset.fractionY).toInt()
    }
}

fun Player.getLookingAt(distance: Double): HitResult? {
    val blockHit = this.pick(distance, 0f, CONFIG.showFluids) as? BlockHitResult

    val startPos = this.getEyePosition(0f)
    val lookVec = this.getViewVector(0f)
    val endPos = startPos.add(lookVec.scale(distance))

    val expandedBox = this.boundingBox.inflate(distance) // Expand around player

    val entityHit = this.level().getEntities(this, expandedBox)
        .mapNotNull { entity ->
            val entityBox = entity.boundingBox.inflate(0.25) // Slightly expand hitbox
            val optionalHit = entityBox.clip(startPos, endPos)
            optionalHit.takeIf { optionalHit.isPresent }
                ?.let { EntityHitResult(entity, it.get()) } // If it intersects, create hit result
        }
        .minByOrNull { it.location.distanceToSqr(startPos) } // Find the closest entity

    return when {
        blockHit != null && entityHit != null -> {
            val blockDist = blockHit.location.distanceToSqr(startPos)
            val entityDist = entityHit.location.distanceToSqr(startPos)
            if (entityDist < blockDist) entityHit else blockHit
        }

        blockHit != null -> blockHit
        entityHit != null -> entityHit
        else -> null
    }
}