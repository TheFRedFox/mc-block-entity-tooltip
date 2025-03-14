package com.thefredfox.minecraft.plugins.blockentitytooltip

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.ColorHelper
import net.minecraft.world.RaycastContext
import org.slf4j.LoggerFactory

lateinit var CONFIG: BlockEntityTooltipModConfig

lateinit var CONFIG_HOLDER: ConfigHolder<BlockEntityTooltipModConfig>

object BlockEntityTooltipClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onInitializeClient() {
        AutoConfig.register(BlockEntityTooltipModConfig::class.java, ::GsonConfigSerializer)
        CONFIG_HOLDER = AutoConfig.getConfigHolder(BlockEntityTooltipModConfig::class.java)
        CONFIG_HOLDER.registerSaveListener { configHolder, config ->
            CONFIG = config
            ActionResult.SUCCESS
        }
        CONFIG = CONFIG_HOLDER.config
        HudRenderCallback.EVENT.register(LookingAtRenderer())
    }
}

fun getNameOfLookedAt(client: MinecraftClient, distance: Double = 5.0): String? {
    val hitResult = client.player?.getLookingAt2(distance)
        ?: return null
//    val hitResult = client.player?.getLookingAt(distance)
//        ?: return null
//    val hitResult = client.crosshairTarget
    when (hitResult.type) {
        HitResult.Type.BLOCK -> {
            if (CONFIG.showBlocks) {
                val cHitResult = hitResult as BlockHitResult
                val blockPos = cHitResult.blockPos
                val blockState = client.world!!.getBlockState(blockPos)
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

class LookingAtRenderer : HudRenderCallback {
    override fun onHudRender(drawContext: DrawContext?, tickCounter: RenderTickCounter?) {
        drawContext?.let {
            val client = MinecraftClient.getInstance()

            getNameOfLookedAt(client, CONFIG.distance)?.let { text ->
                val textRenderer: TextRenderer = client.textRenderer
                val textObj = Text.literal(text)

                val screenWidth = drawContext.scaledWindowWidth
                val screenHeight = drawContext.scaledWindowHeight

                val padding = 4

                val textWidth = textRenderer.getWidth(textObj)
                val textHeight = textRenderer.fontHeight

                val x = screenWidth - textWidth - 10
                val y = screenHeight - textHeight - 40
                val textColor = 0xFFFFFF

                // Background box coordinates
                val bgX1 = x - padding
                val bgY1 = y - padding
                val bgX2 = x + textWidth + padding
                val bgY2 = y + textHeight + padding

                val bgColor = ColorHelper.getArgb(0x88, 0x0, 0x0, 0x0) // Black with transparency

                // Draw background box
                drawContext.fill(bgX1, bgY1, bgX2, bgY2, bgColor)

                // Draw text on top
                drawContext.drawTextWithShadow(textRenderer, text, x, y, textColor) // White text
            }

        }
    }
}

fun PlayerEntity.getLookingAt(distance: Double = 5.0): BlockHitResult? {
    // Define max distance for raycast (adjust as needed)
    val maxDistance = distance

    val cameraPos = this.getCameraPosVec(1.0f)
    val lookVec = this.getRotationVec(1.0f)
    val endPos = cameraPos.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance)

    return world.raycast(
        RaycastContext(
            cameraPos, endPos,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            this
        )
    )
}

fun PlayerEntity.getLookingAt2(distance: Double): HitResult? {
    // 1️⃣ Perform a block raycast
    val blockHit = this.raycast(distance, 0f, CONFIG.showFluids) as? BlockHitResult

    // 2️⃣ Perform an entity raycast
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

    // 3️⃣ Compare distances (if both exist)
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