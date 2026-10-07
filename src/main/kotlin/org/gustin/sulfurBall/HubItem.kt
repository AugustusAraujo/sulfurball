package org.gustin.sulfurBall

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object HubItem {

    private fun key(plugin: SulfurBall): NamespacedKey = NamespacedKey(plugin, "hub_item")

    fun give(plugin: SulfurBall, player: Player) {
        removeFrom(plugin, player)
        val item = ItemStack.of(Material.NETHER_STAR)
        item.editMeta { meta ->
            meta.displayName(Component.text("Hub", NamedTextColor.AQUA))
            meta.lore(listOf(Component.text("Clique para voltar ao hub", NamedTextColor.GRAY)))
            meta.persistentDataContainer.set(key(plugin), PersistentDataType.BOOLEAN, true)
        }
        player.inventory.addItem(item)
    }

    fun isHubItem(plugin: SulfurBall, item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        if (!item.hasItemMeta()) return false
        return item.itemMeta.persistentDataContainer.has(key(plugin), PersistentDataType.BOOLEAN)
    }

    fun removeFrom(plugin: SulfurBall, player: Player) {
        player.inventory.contents.filterNotNull()
            .filter { isHubItem(plugin, it) }
            .forEach { it.amount = 0 }
        if (isHubItem(plugin, player.inventory.itemInOffHand)) {
            player.inventory.setItemInOffHand(null)
        }
    }
}
