package org.gustin.sulfurBall

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object PerkSelector {

    private fun key(plugin: SulfurBall): NamespacedKey = NamespacedKey(plugin, "perk_selector")

    fun give(plugin: SulfurBall, player: Player) {
        removeFrom(plugin, player)
        val item = ItemStack.of(Material.CHEST)
        item.editMeta { meta ->
            meta.displayName(Component.text("Perks", NamedTextColor.GOLD))
            meta.lore(listOf(Component.text("Clique para escolher sua perk", NamedTextColor.GRAY)))
            meta.persistentDataContainer.set(key(plugin), PersistentDataType.BOOLEAN, true)
        }
        player.inventory.addItem(item)
    }

    fun isSelectorItem(plugin: SulfurBall, item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        if (!item.hasItemMeta()) return false
        return item.itemMeta.persistentDataContainer.has(key(plugin), PersistentDataType.BOOLEAN)
    }

    fun removeFrom(plugin: SulfurBall, player: Player) {
        player.inventory.contents.filterNotNull()
            .filter { isSelectorItem(plugin, it) }
            .forEach { it.amount = 0 }
        if (isSelectorItem(plugin, player.inventory.itemInOffHand)) {
            player.inventory.setItemInOffHand(null)
        }
    }
}
