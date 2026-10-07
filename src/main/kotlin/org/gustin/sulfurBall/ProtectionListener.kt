package org.gustin.sulfurBall

import org.bukkit.NamespacedKey
import org.bukkit.entity.Firework
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ProtectionListener(private val plugin: SulfurBall) : Listener {

    private val armorKey: NamespacedKey = NamespacedKey(plugin, "match_armor")

    private fun isProtectedItem(item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        if (HubItem.isHubItem(plugin, item)) return true
        if (PerkManager.isPerkItem(plugin, item)) return true
        if (PerkSelector.isSelectorItem(plugin, item)) return true
        if (!item.hasItemMeta()) return false
        return item.itemMeta.persistentDataContainer.has(armorKey, PersistentDataType.BOOLEAN)
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (event.damager is Firework) event.isCancelled = true
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if (isProtectedItem(event.itemDrop.itemStack)) event.isCancelled = true
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (isProtectedItem(event.currentItem) || isProtectedItem(event.cursor)) {
            event.isCancelled = true
            return
        }
        val hotbarButton = event.hotbarButton
        if (hotbarButton >= 0) {
            val swapped = event.whoClicked.inventory.getItem(hotbarButton)
            if (isProtectedItem(swapped)) event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (isProtectedItem(event.oldCursor)) {
            event.isCancelled = true
            return
        }
        if (event.newItems.values.any { isProtectedItem(it) }) event.isCancelled = true
    }
}
