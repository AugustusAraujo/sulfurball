package org.gustin.sulfurBall

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class PerkMenu(plugin: SulfurBall, selectedPerk: String?) : InventoryHolder {

    private val menu: Inventory = Bukkit.createInventory(this, 9, Component.text("Perks", NamedTextColor.DARK_AQUA))

    init {
        PerkManager.perks.forEachIndexed { i, perk ->
            val item = perk.buildItem(plugin)
            item.editMeta { meta ->
                val extra = if (perk.id == selectedPerk) {
                    Component.text("✔ Selecionada", NamedTextColor.GREEN)
                } else {
                    Component.text("Clique para selecionar", NamedTextColor.YELLOW)
                }
                meta.lore(meta.lore().orEmpty() + extra)
            }
            menu.setItem(i, item)
        }
    }

    override fun getInventory(): Inventory = menu

    fun open(player: Player) {
        player.openInventory(menu)
    }
}
