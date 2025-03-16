package com.thefredfox.minecraft.plugins.blockentitytooltip

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import net.minecraft.fluid.Fluids

@Config(name = "block-entity-tooltip")
class BlockEntityTooltipModConfig(
    val enabled: Boolean = true,
    var distance: Double = 5.0,
    var showBlocks: Boolean = true,
    var showEntities: Boolean = true,
    var showFluids: Boolean = false,
) : ConfigData
