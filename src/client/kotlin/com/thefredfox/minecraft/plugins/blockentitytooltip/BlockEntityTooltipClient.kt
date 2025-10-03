package com.thefredfox.minecraft.plugins.blockentitytooltip

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.ColorHelper
import org.slf4j.LoggerFactory

lateinit var CONFIG: BlockEntityTooltipModConfig

lateinit var CONFIG_HOLDER: ConfigHolder<BlockEntityTooltipModConfig>

object BlockEntityTooltipClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val LAYER_IDENTIFIER: Identifier = Identifier.of("block_entity_tooltip", "looking_at")
    private val lookingAtRenderer = LookingAtRenderer()

    override fun onInitializeClient() {
        AutoConfig.register(BlockEntityTooltipModConfig::class.java, ::GsonConfigSerializer)
        CONFIG_HOLDER = AutoConfig.getConfigHolder(BlockEntityTooltipModConfig::class.java)
        CONFIG_HOLDER.registerSaveListener { configHolder, config ->
            CONFIG = config
            ActionResult.SUCCESS
        }
        CONFIG = CONFIG_HOLDER.config
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CROSSHAIR,
            LAYER_IDENTIFIER,
            lookingAtRenderer::render
        )
    }
}

fun getNameOfLookedAt(player: PlayerEntity, distance: Double = 5.0): String? {
    val hitResult = player.getLookingAt(distance)
        ?: return null

    when (hitResult.type) {
        HitResult.Type.BLOCK -> {
            if (CONFIG.showBlocks) {
                val cHitResult = hitResult as BlockHitResult
                val blockPos = cHitResult.blockPos
                val blockState = player.world.getBlockState(blockPos)
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
    override fun render(drawContext: DrawContext, tickCounter: RenderTickCounter) {
        if (!CONFIG.enabled) {
            return
        }

        val client = MinecraftClient.getInstance()
            ?: return
        val player = client.player
            ?: return
        val world = client.world
            ?: return

        getNameOfLookedAt(player, CONFIG.distance)?.let { text ->
            val textRenderer = client.textRenderer
            val textObj = Text.literal(text)

            val screenWidth = drawContext.scaledWindowWidth
            val screenHeight = drawContext.scaledWindowHeight

            val padding = 4

            val textWidth = textRenderer.getWidth(textObj)
            val textHeight = textRenderer.fontHeight

            val x = screenWidth - textWidth - 10
            val y = screenHeight - textHeight - 40

            // Background box coordinates
            val bgX1 = x - padding
            val bgY1 = y - padding
            val bgX2 = x + textWidth + padding
            val bgY2 = y + textHeight + padding

            val bgColor = ColorHelper.getArgb(0x88, 0x0, 0x0, 0x0) // Black with transparency
            val textColor = 0xFFFFFFFF.toInt() // White with full alpha

            // Draw background box first
            drawContext.fill(bgX1, bgY1, bgX2, bgY2, bgColor)

            // Draw text with shadow
            drawContext.drawTextWithShadow(textRenderer, textObj, x, y, textColor)
        }
    }
}

fun PlayerEntity.getLookingAt(distance: Double): HitResult? {
    val blockHit = this.raycast(distance, 0f, CONFIG.showFluids) as? BlockHitResult

    val startPos = this.getCameraPosVec(0f)
    val lookVec = this.getRotationVec(0f)
    val endPos = startPos.add(lookVec.multiply(distance))

    val expandedBox = this.boundingBox.expand(distance) // Expand around player

    val entityHit = world.getOtherEntities(this, expandedBox)
        .mapNotNull { entity ->
            val entityBox = entity.boundingBox.expand(0.25) // Slightly expand hitbox
            val optionalHit = entityBox.raycast(startPos, endPos)
            optionalHit.takeIf { optionalHit.isPresent }
                ?.let { EntityHitResult(entity, it.get()) } // If it intersects, create hit result
        }
        .minByOrNull { it.pos.squaredDistanceTo(startPos) } // Find the closest entity

    return when {
        blockHit != null && entityHit != null -> {
            val blockDist = blockHit.pos.squaredDistanceTo(startPos)
            val entityDist = entityHit.pos.squaredDistanceTo(startPos)
            if (entityDist < blockDist) entityHit else blockHit
        }

        blockHit != null -> blockHit
        entityHit != null -> entityHit
        else -> null
    }
}