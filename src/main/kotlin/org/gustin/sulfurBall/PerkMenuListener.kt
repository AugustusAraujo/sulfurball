package org.gustin.sulfurBall

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEvent

class PerkMenuListener(private val plugin: SulfurBall) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK &&
            action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
        ) return

        if (!PerkSelector.isSelectorItem(plugin, event.item)) return

        event.isCancelled = true
        this.openMenu(event.player)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.view.topInventory.holder !is PerkMenu) return
        event.isCancelled = true

        val item = event.currentItem ?: return
        val perk = PerkManager.find(plugin, item) ?: return
        val player = event.whoClicked as Player

        plugin.database.setSelectedPerk(player.uniqueId, perk.id)
        player.sendMessage(Component.text("Perk ${perk.displayName} selecionada!", NamedTextColor.GREEN))
        player.closeInventory()
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is PerkMenu) event.isCancelled = true
    }

    private fun openMenu(player: Player) {
        if (plugin.database.getTeam(player.uniqueId) != null) {
            player.sendMessage(Component.text("Você não pode trocar de perk durante a partida.", NamedTextColor.RED))
            return
        }
        PerkMenu(plugin, plugin.database.getSelectedPerk(player.uniqueId)).open(player)
    }
}
