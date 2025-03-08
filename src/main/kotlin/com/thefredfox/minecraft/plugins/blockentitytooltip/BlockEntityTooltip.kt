package com.thefredfox.minecraft.plugins.blockentitytooltip

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory


object BlockEntityTooltip : ModInitializer {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Hello Fabric world!")
        //ClientTickEvents.END_CLIENT_TICK: Called at the end of the client tick.
        //ClientTickEvents.END_WORLD_TICK: Called at the end of the ClientWorld's tick.
        //ClientTickEvents.START_CLIENT_TICK: Called at the start of the client tick.
        //ClientTickEvents.START_WORLD_TICK: Called at the start of the ClientWorld's tick.

//        AttackBlockCallback.EVENT.register(AttackBlockCallback { player: PlayerEntity, world: World, hand: Hand?, pos: BlockPos?, direction: Direction? ->
//            val state = world.getBlockState(pos)
            /* Manual spectator check is necessary because AttackBlockCallbacks
               fire before the spectator check */
//            if (state.isToolRequired && !player.isSpectator &&
//                player.mainHandStack.isEmpty
//            ) {
//                player.damage(DamageSource.field_5869, 1.0f)
//            }
//            ActionResult.PASS
//        })
    }
}