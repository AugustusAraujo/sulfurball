package org.gustin.sulfurBall

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinListener(private val plugin: SulfurBall) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (plugin.database.getTeam(player.uniqueId) != null) return

        player.inventory.clear()
        plugin.getSpawn("hub")?.let { player.teleport(it) }

        if (plugin.database.getLobbyPlayers().contains(player.uniqueId)) {
            HubItem.give(plugin, player)
        }
    }
}
