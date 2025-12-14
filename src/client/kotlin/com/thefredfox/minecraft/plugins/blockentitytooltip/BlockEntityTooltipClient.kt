package com.thefredfox.minecraft.plugins.blockentitytooltip

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
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
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CROSSHAIR,
            LAYER_IDENTIFIER,
            lookingAtRenderer::render
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
    override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
        if (!CONFIG.enabled) {
            return
        }

        val client = Minecraft.getInstance()
            ?: return
        val player = client.player
            ?: return
        val world = client.level
            ?: return

        getNameOfLookedAt(player, CONFIG.distance)?.let { text ->
            val font = client.font
            val textObj = Component.literal(text)

            val screenWidth = guiGraphics.guiWidth()
            val screenHeight = guiGraphics.guiHeight()

            val padding = 4

            val textWidth = font.width(textObj)
            val textHeight = font.lineHeight

            val x = screenWidth - textWidth - 10
            val y = screenHeight - textHeight - 40

            // Background box coordinates
            val bgX1 = x - padding
            val bgY1 = y - padding
            val bgX2 = x + textWidth + padding
            val bgY2 = y + textHeight + padding

            val bgColor = ARGB.color(0x88, 0x0, 0x0, 0x0) // Black with transparency
            val textColor = 0xFFFFFFFF.toInt() // White with full alpha

            // Draw background box first
            guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, bgColor)

            // Draw text with shadow
            guiGraphics.drawString(font, textObj, x, y, textColor)
        }
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