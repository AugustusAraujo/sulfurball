package org.gustin.sulfurBall

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

class SulfurBall : JavaPlugin() {

    lateinit var database: Database
        private set

    var matchRunning = false

    val matchEvents = MatchEvents(this)

    override fun onEnable() {
        database = Database(this)
        for (world in Bukkit.getWorlds()) {
            world.setGameRule(GameRules.LOCATOR_BAR, false)
            world.setGameRule(GameRules.PVP, false)
            world.setGameRule(GameRules.ADVANCE_TIME, false)
            world.setGameRule(GameRules.ADVANCE_WEATHER, false)
            world.setGameRule(GameRules.MOB_GRIEFING, false)
            world.setGameRule(GameRules.SPAWN_MOBS, false)
        }

        database.clearLobby()
        database.clearTeams()

        server.pluginManager.registerEvents(HubItemListener(this), this)
        server.pluginManager.registerEvents(JoinListener(this), this)
        server.pluginManager.registerEvents(ProtectionListener(this), this)
        server.pluginManager.registerEvents(PerkListener(this), this)
        server.pluginManager.registerEvents(PerkMenuListener(this), this)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(MatchCommands(this, matchEvents).build(), "Manage the match")
            event.registrar().register(HubCommands(this).build(), "Teleport to the hub")
            event.registrar().register(PerkCommands(this).build(), "Escolher perk")
        }

        val world = Bukkit.getWorld("world")
        val goalRed = AreaWatcher(
            plugin = this,
            tag = "sulfurball",
            corner1 = Location(world, -113.0,150.0,-51.0),
            corner2 = Location(world, -113.0,152.0,-47.0),
            onEnter = { ball ->
                matchEvents.onGoal(Team.BLUE, ball)
            }
        )
        val goalBlue = AreaWatcher(
            plugin = this,
            tag = "sulfurball",
            corner1 = Location(world, -64.0,150.0,-51.0),
            corner2 = Location(world, -64.0,152.0,-47.0),
            onEnter = { ball ->
                matchEvents.onGoal(Team.RED, ball)
            }
        )

        goalRed.start()
        goalBlue.start()

        matchEvents.startLobbyWatcher()
    }

    override fun onDisable() {
        if (::database.isInitialized) database.close()
    }

    fun setSpawn(name: String, location: Location) {
        config.set("spawns.$name", location)
        saveConfig()
    }

    fun getSpawn(name: String): Location? = config.getLocation("spawns.$name")
}