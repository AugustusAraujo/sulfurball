package org.gustin.sulfurBall

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object PerkManager {

    val perks: List<Perk> = listOf(SpeedPerk, ExplosionPerk, StrengthPerk, FreezePerk)

    fun perkKey(plugin: SulfurBall): NamespacedKey = NamespacedKey(plugin, "perk")

    fun giveSelectedOrRandom(plugin: SulfurBall, player: Player) {
        val selectedId = plugin.database.getSelectedPerk(player.uniqueId)
        val perk = perks.firstOrNull { it.id == selectedId } ?: perks.random()
        if (selectedId == null) {
            player.sendActionBar(Component.text("Perk aleatória: ${perk.displayName}", NamedTextColor.GREEN))
        }
        player.inventory.setItem(perk.slot, perk.buildItem(plugin))
        player.setCooldown(perk.material, perk.chargeTicks)
        this.scheduleReadyNotice(plugin, player, perk)
    }

    fun isPerkItem(plugin: SulfurBall, item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        if (!item.hasItemMeta()) return false
        return item.itemMeta.persistentDataContainer.has(perkKey(plugin), PersistentDataType.STRING)
    }

    fun clearCooldowns(player: Player) {
        perks.forEach { player.setCooldown(it.material, 0) }
    }

    fun activate(plugin: SulfurBall, player: Player, item: ItemStack) {
        val perk = this.find(plugin, item) ?: return

        if (plugin.matchEvents.isPreparing()) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            player.sendActionBar(Component.text("Aguarde o reinício da partida.", NamedTextColor.YELLOW))
            return
        }

        val cooldown = player.getCooldown(perk.material)
        if (cooldown > 0) {
            val seconds = cooldown / 20 + 1
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            player.sendActionBar(
                Component.text("${perk.displayName} recarregando: ${seconds}s", NamedTextColor.YELLOW)
            )
            return
        }

        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
        perk.onActivate(plugin, player)
        player.setCooldown(perk.material, perk.chargeTicks)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (plugin.database.getTeam(player.uniqueId) == null) return@Runnable
            perk.onExpire(plugin, player)
        }, perk.boostTicks.toLong())
        this.scheduleReadyNotice(plugin, player, perk)
    }

    fun find(plugin: SulfurBall, item: ItemStack): Perk? {
        if (!item.hasItemMeta()) return null
        val id = item.itemMeta.persistentDataContainer.get(perkKey(plugin), PersistentDataType.STRING) ?: return null
        return perks.firstOrNull { it.id == id }
    }

    private fun scheduleReadyNotice(plugin: SulfurBall, player: Player, perk: Perk) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (!player.isOnline) return@Runnable
            if (plugin.database.getTeam(player.uniqueId) == null) return@Runnable
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f)
            player.sendActionBar(Component.text("${perk.displayName} pronta!", NamedTextColor.GREEN))
        }, perk.chargeTicks.toLong())
    }
}
