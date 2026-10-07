package org.gustin.sulfurBall

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.BoundingBox
import java.util.UUID

class AreaWatcher(
    private val plugin: JavaPlugin,
    private val tag: String,
    corner1: Location,
    corner2: Location,
    private val onEnter: (Entity) -> Unit,
    private val onExit: (Entity) -> Unit = {}
) {
    private val world = corner1.world
    private val area = BoundingBox.of(corner1.block, corner2.block) // cobre os blocos inteiros
    private val inside = mutableSetOf<UUID>()
    private var task: BukkitTask? = null

    fun start() {
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 0L, 2L)
    }

    fun stop() {
        task?.cancel()
        inside.clear()
    }

    private fun tick() {
        val current = world.getNearbyEntities(area) { it.scoreboardTags.contains(tag) }
        val currentIds = current.map { it.uniqueId }.toSet()

        // entraram agora
        current.filter { inside.add(it.uniqueId) }.forEach(onEnter)

        // saíram
        val left = inside.filter { it !in currentIds }
        left.forEach { id ->
            inside.remove(id)
            plugin.server.getEntity(id)?.let(onExit)
        }
    }
}