package org.gustin.sulfurBall

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class HubItemListener(private val plugin: SulfurBall) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK &&
            action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
        ) return

        if (!HubItem.isHubItem(plugin, event.item)) return

        event.isCancelled = true
        HubItem.sendToHub(plugin, event.player)
    }
}
