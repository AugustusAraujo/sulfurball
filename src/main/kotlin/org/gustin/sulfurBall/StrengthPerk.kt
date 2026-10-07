package org.gustin.sulfurBall

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object StrengthPerk : Perk(
    id = "strength",
    displayName = "Força",
    material = Material.BLAZE_ROD,
    chargeTicks = 20 * 30,
    boostTicks = 20 * 10,
    slot = 8,
) {
    override fun onActivate(plugin: SulfurBall, player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, boostTicks, 1))
    }
}
