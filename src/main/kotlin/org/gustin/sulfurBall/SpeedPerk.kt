package org.gustin.sulfurBall

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object SpeedPerk : Perk(
    id = "speed",
    displayName = "Turbo",
    material = Material.SUGAR,
    chargeTicks = 20 * 30,
    boostTicks = 20 * 5,
    slot = 8,
) {
    override fun onActivate(plugin: SulfurBall, player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, boostTicks, 3))
    }
}
