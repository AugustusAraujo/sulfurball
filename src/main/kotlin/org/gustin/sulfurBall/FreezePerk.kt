package org.gustin.sulfurBall

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object FreezePerk : Perk(
    id = "freeze",
    displayName = "Congelar",
    material = Material.PACKED_ICE,
    chargeTicks = 20 * 30,
    boostTicks = 20 * 5,
    slot = 8,
) {
    private const val RADIUS = 5.0

    override fun onActivate(plugin: SulfurBall, player: Player) {
        val myTeam = plugin.database.getTeam(player.uniqueId) ?: return
        val otherTeam = Team.entries.first { it.name != myTeam }

        val victims = plugin.database.getTeamPlayers(otherTeam.name)
            .mapNotNull { Bukkit.getPlayer(it) }
            .filter { it.world == player.world && it.location.distance(player.location) <= RADIUS }

        victims.forEach { victim ->
            victim.inventory.setHelmet(ItemStack.of(Material.PACKED_ICE))
            victim.setFreezeTicks(victim.maxFreezeTicks + boostTicks)
            victim.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, boostTicks, 250, true, false, true))
            victim.playSound(victim.location, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f)
        }

        if (victims.isEmpty()) return

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            victims.forEach { victim ->
                if (victim.isOnline) plugin.matchEvents.restoreHelmet(victim)
            }
        }, boostTicks.toLong())
    }

    override fun onExpire(plugin: SulfurBall, player: Player) {}
}
