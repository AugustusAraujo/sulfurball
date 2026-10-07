package org.gustin.sulfurBall

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class PerkCommands(private val plugin: SulfurBall) {

    fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("perk")
            .executes { perk(it) }
            .build()

    private fun perk(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as? Player ?: run {
            ctx.source.sender.sendMessage(Component.text("Only players can use this.", NamedTextColor.RED))
            return 0
        }

        if (plugin.database.getTeam(player.uniqueId) != null) {
            player.sendMessage(Component.text("Você não pode trocar de perk durante a partida.", NamedTextColor.RED))
            return 0
        }

        PerkMenu(plugin, plugin.database.getSelectedPerk(player.uniqueId)).open(player)
        return Command.SINGLE_SUCCESS
    }
}
