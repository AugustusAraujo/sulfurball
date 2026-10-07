package org.gustin.sulfurBall

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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

        val hub = plugin.getSpawn("hub")
        if (hub == null) {
            event.player.sendMessage(
                Component.text("Hub não configurado. Use /match setspawn hub.", NamedTextColor.RED)
            )
            return
        }

        event.player.teleport(hub)
        event.player.sendMessage(Component.text("Teleportado para o hub.", NamedTextColor.GREEN))
    }
}
