package org.gustin.sulfurBall

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector

object ExplosionPerk : Perk(
    id = "explosion",
    displayName = "Explosão",
    material = Material.TNT,
    chargeTicks = 20 * 30,
    boostTicks = 20,
    slot = 8,
) {
    private const val RADIUS = 5.0
    private const val KNOCKBACK = 1.5
    private const val KNOCKBACK_Y = 0.5

    override fun onActivate(plugin: SulfurBall, player: Player) {
        val loc = player.location
        loc.world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1)
        loc.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)

        val myTeam = plugin.database.getTeam(player.uniqueId) ?: return
        val otherTeam = Team.entries.first { it.name != myTeam }
        plugin.database.getTeamPlayers(otherTeam.name)
            .mapNotNull { Bukkit.getPlayer(it) }
            .filter { it.world == loc.world && it.location.distance(loc) <= RADIUS }
            .forEach { enemy ->
                val dir = enemy.location.toVector().subtract(loc.toVector())
                dir.y = 0.0
                if (dir.lengthSquared() > 0.01) {
                    enemy.velocity = dir.normalize().multiply(KNOCKBACK).setY(KNOCKBACK_Y)
                }
            }
    }

    override fun onExpire(plugin: SulfurBall, player: Player) {}
}
