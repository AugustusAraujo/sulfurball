package org.gustin.sulfurBall

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player

enum class Team { RED, BLUE }

class MatchCommands(private val plugin: SulfurBall, private val matchEvents: MatchEvents) {
    private val db get() = plugin.database
    private val adminPerm = "sulfurball.admin"
    private val spawnNames = listOf("lobby", "ball", "hub") + Team.entries.map { it.name.lowercase() }

    fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("match")
            .then(Commands.literal("join").executes { join(it) })
            .then(Commands.literal("leave").executes { leave(it) })
            .then(Commands.literal("players").executes { players(it) })
            .then(
                Commands.literal("start")
                    .requires { it.sender.hasPermission(adminPerm) }
                    .executes { start(it) }
            )
            .then(
                Commands.literal("end")
                    .requires { it.sender.hasPermission(adminPerm) }
                    .executes { end(it) }
            )
            .then(
                Commands.literal("setspawn")
                    .requires { it.sender.hasPermission(adminPerm) }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                            .suggests { _, builder ->
                                spawnNames.forEach { builder.suggest(it) }
                                builder.buildFuture()
                            }
                            .executes { setSpawn(it) }
                    )
            )
            .build()

    // ---------- handlers ----------

    private fun join(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as? Player ?: return playersOnly(ctx)

        if (plugin.matchRunning) return error(ctx, "A match is already running.")

        db.addLobbyPlayer(player.uniqueId)
        plugin.getSpawn("lobby")?.let { player.teleport(it) }
        HubItem.give(plugin, player)
        player.sendMessage(Component.text("You joined the lobby.", NamedTextColor.GREEN))
        matchEvents.tryStartLobbyCountdown()
        return Command.SINGLE_SUCCESS
    }

    private fun leave(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as? Player ?: return playersOnly(ctx)

        db.removeLobbyPlayer(player.uniqueId)
        HubItem.removeFrom(plugin, player)
        player.sendMessage(Component.text("You left the lobby.", NamedTextColor.YELLOW))
        return Command.SINGLE_SUCCESS
    }

    private fun players(ctx: CommandContext<CommandSourceStack>): Int {
        val list = db.getLobbyPlayers()
        if (list.isEmpty()) {
            ctx.source.sender.sendMessage(Component.text("The lobby is empty.", NamedTextColor.GRAY))
        } else {
            val names = list.joinToString(", ") { Bukkit.getOfflinePlayer(it).name ?: it.toString() }
            ctx.source.sender.sendMessage(
                Component.text("Lobby (${list.size}): $names", NamedTextColor.AQUA)
            )
        }
        return Command.SINGLE_SUCCESS
    }

    private fun setSpawn(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as? Player ?: return playersOnly(ctx)
        val name = StringArgumentType.getString(ctx, "name").lowercase()

        if (name !in spawnNames) {
            return error(ctx, "Unknown spawn '$name'. Options: ${spawnNames.joinToString()}")
        }

        plugin.setSpawn(name, player.location)
        player.sendMessage(Component.text("Spawn '$name' set to your current position.", NamedTextColor.GREEN))
        return Command.SINGLE_SUCCESS
    }

    private fun start(ctx: CommandContext<CommandSourceStack>): Int {
        if (plugin.matchRunning) return error(ctx, "A match is already running.")

        val missing = Team.entries.map { it.name.lowercase() }.filter { plugin.getSpawn(it) == null }
        if (missing.isNotEmpty()) {
            return error(ctx, "Missing spawns: ${missing.joinToString()}. Use /match setspawn <name>.")
        }

        val online = db.getLobbyPlayers().mapNotNull { Bukkit.getPlayer(it) }
        if (online.size < Team.entries.size) {
            return error(ctx, "Not enough players in the lobby (need at least ${Team.entries.size}).")
        }

        matchEvents.cancelLobbyCountdown()
        matchEvents.startMatch()
        return Command.SINGLE_SUCCESS
    }

    private fun end(ctx: CommandContext<CommandSourceStack>): Int {
        if (!plugin.matchRunning) return error(ctx, "No match is running.")

        matchEvents.endMatch()
        return Command.SINGLE_SUCCESS
    }

    private fun error(ctx: CommandContext<CommandSourceStack>, message: String): Int {
        ctx.source.sender.sendMessage(Component.text(message, NamedTextColor.RED))
        return 0
    }

    private fun playersOnly(ctx: CommandContext<CommandSourceStack>): Int =
        error(ctx, "Only players can use this.")
}