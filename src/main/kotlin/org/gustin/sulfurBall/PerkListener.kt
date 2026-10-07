package org.gustin.sulfurBall

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class PerkListener(private val plugin: SulfurBall) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK &&
            action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
        ) return

        val item = event.item ?: return
        if (!PerkManager.isPerkItem(plugin, item)) return

        event.isCancelled = true
        PerkManager.activate(plugin, event.player, item)
    }
}
