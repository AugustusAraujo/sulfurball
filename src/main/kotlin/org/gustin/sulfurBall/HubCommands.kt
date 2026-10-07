package org.gustin.sulfurBall

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player

class HubCommands(private val plugin: SulfurBall, private val matchEvents: MatchEvents) {

    fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("hub")
            .executes { hub(it) }
            .build()

    private fun hub(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as? Player ?: run {
            ctx.source.sender.sendMessage(Component.text("Only players can use this.", NamedTextColor.RED))
            return 0
        }

        val hub = plugin.getSpawn("hub")
        if (hub == null) {
            player.sendMessage(Component.text("Hub não configurado. Use /match setspawn hub.", NamedTextColor.RED))
            return 0
        }

        if (plugin.database.getTeam(player.uniqueId) != null) {
            plugin.database.removeFromTeam(player.uniqueId)
            player.inventory.clear()
            player.clearActivePotionEffects()
            matchEvents.hideScoreBar(player)
            player.gameMode = GameMode.ADVENTURE

            if (plugin.matchRunning && this.noPlayersLeftInTeams()) {
                matchEvents.endMatch()
            }
        }

        plugin.database.removeLobbyPlayer(player.uniqueId)
        HubItem.removeFrom(plugin, player)

        player.teleport(hub)
        player.sendMessage(Component.text("Teleportado para o hub.", NamedTextColor.GREEN))
        return Command.SINGLE_SUCCESS
    }

    private fun noPlayersLeftInTeams(): Boolean =
        Team.entries
            .flatMap { plugin.database.getTeamPlayers(it.name) }
            .mapNotNull { Bukkit.getPlayer(it) }
            .none()
}
