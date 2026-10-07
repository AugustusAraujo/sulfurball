package org.gustin.sulfurBall

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

abstract class Perk(
    val id: String,
    val displayName: String,
    val material: Material,
    val chargeTicks: Int,
    val boostTicks: Int,
    val slot: Int,
) {
    open fun lore(): List<Component> = listOf(
        Component.text("Clique para ativar", NamedTextColor.GRAY),
        Component.text("Duração: ${boostTicks / 20}s • Recarga: ${chargeTicks / 20}s", NamedTextColor.DARK_GRAY)
    )

    fun buildItem(plugin: SulfurBall): ItemStack {
        val item = ItemStack.of(material)
        item.editMeta { meta ->
            meta.displayName(Component.text(displayName, NamedTextColor.AQUA))
            meta.lore(lore())
            meta.persistentDataContainer.set(PerkManager.perkKey(plugin), PersistentDataType.STRING, id)
        }
        return item
    }

    abstract fun onActivate(plugin: SulfurBall, player: Player)

    open fun onExpire(plugin: SulfurBall, player: Player) {
        plugin.matchEvents.applyPlayerEffects(player)
    }
}
