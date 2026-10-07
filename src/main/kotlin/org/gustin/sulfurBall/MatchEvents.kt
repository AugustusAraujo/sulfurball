package org.gustin.sulfurBall

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.entity.SulfurCube
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.time.Duration

class MatchEvents(private val plugin: SulfurBall) {
    val ballTag = "sulfurball"
    private var freezeTask: BukkitTask? = null
    private var countdownTask: BukkitTask? = null
    private var lobbyCountdownTask: BukkitTask? = null
    private var matchTimerTask: BukkitTask? = null
    private var clockTask: BukkitTask? = null
    private val scoreBar: BossBar = BossBar.bossBar(
        Component.text("RED 0 x 0 BLUE", NamedTextColor.WHITE),
        1.0f,
        BossBar.Color.WHITE,
        BossBar.Overlay.PROGRESS
    )

    fun onGoal(team: Team, ball: Entity) {
        this.detonateBall(ball.location, team)
        ball.remove()
        plugin.database.addPoint(team.name)
        scoreBar.name(scoreBarName())
        sendGoalMessage(team)
        this.startFreeze()
    }

    fun tryStartLobbyCountdown() {
        if (plugin.matchRunning) return
        if (lobbyCountdownTask != null) return

        val missing = (Team.entries.map { it.name.lowercase() } + "ball").filter { plugin.getSpawn(it) == null }
        if (missing.isNotEmpty()) return

        val online = plugin.database.getLobbyPlayers().mapNotNull { Bukkit.getPlayer(it) }
        if (online.size < Team.entries.size) return

        var secondsLeft = 10
        lobbyCountdownTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (plugin.matchRunning) {
                this.cancelLobbyCountdown()
                return@Runnable
            }
            val players = plugin.database.getLobbyPlayers().mapNotNull { Bukkit.getPlayer(it) }
            if (players.size < Team.entries.size) {
                this.cancelLobbyCountdown()
                players.forEach {
                    it.sendMessage(Component.text("Countdown cancelado: jogadores insuficientes.", NamedTextColor.RED))
                }
                return@Runnable
            }
            if (secondsLeft > 0) {
                if (secondsLeft == 10) {
                    players.forEach {
                        it.sendMessage(Component.text("A partida começa em 10 segundos...", NamedTextColor.GOLD))
                    }
                }
                val pitch = 0.6f + (10 - secondsLeft) * 0.15f
                val title = this.numberTitle(Component.text(secondsLeft.toString(), NamedTextColor.YELLOW))
                players.forEach { player ->
                    player.showTitle(title)
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch)
                }
                secondsLeft--
            } else {
                this.cancelLobbyCountdown()
                players.forEach { player ->
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f)
                }
                this.startMatch()
            }
        }, 0L, 20L)
    }

    fun cancelLobbyCountdown() {
        lobbyCountdownTask?.cancel()
        lobbyCountdownTask = null
    }

    fun startLobbyWatcher() {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (plugin.matchRunning) return@Runnable
            if (lobbyCountdownTask != null) return@Runnable

            val online = plugin.database.getLobbyPlayers().mapNotNull { Bukkit.getPlayer(it) }
            if (online.isEmpty()) return@Runnable

            if (online.size >= Team.entries.size) {
                this.tryStartLobbyCountdown()
            } else {
                val missing = Team.entries.size - online.size
                online.forEach {
                    it.sendMessage(
                        Component.text("Faltam $missing jogador(es) para começar a partida.", NamedTextColor.YELLOW)
                    )
                }
            }
        }, 200L, 200L)
    }

    fun startMatch() {
        val online = plugin.database.getLobbyPlayers().mapNotNull { Bukkit.getPlayer(it) }

        plugin.database.clearTeams()
        plugin.database.clearScores()
        online.shuffled().forEachIndexed { i, player ->
            val team = Team.entries[i % Team.entries.size]
            plugin.database.setTeam(player.uniqueId, team.name)
            player.teleport(this.teamSpawnFor(team)!!)
            this.preparePlayer(player, team)
            player.gameMode = GameMode.ADVENTURE
            player.setExperienceLevelAndProgress(0)
            player.sendMessage(Component.text("The match started! You are on team ${team.name}.", NamedTextColor.GOLD))
        }

        plugin.matchRunning = true
        plugin.getSpawn("ball")?.let { spawnBall(it) }
        this.startMatchClock()
    }

    fun endMatch() {
        if (!plugin.matchRunning) return

        this.cancelFreeze()
        this.cancelCountdown()
        this.cancelLobbyCountdown()
        this.stopMatchClock()
        this.hideScoreBar()

        val red = plugin.database.getScore(Team.RED.name)
        val blue = plugin.database.getScore(Team.BLUE.name)
        val winnerColor = when {
            red > blue -> NamedTextColor.RED
            blue > red -> NamedTextColor.BLUE
            else -> NamedTextColor.WHITE
        }
        val winnerMessage = when {
            red > blue -> "RED venceu!"
            blue > red -> "BLUE venceu!"
            else -> "Empate!"
        }
        val tempos = Title.Times.times(
            Duration.ofMillis(300),
            Duration.ofSeconds(2),
            Duration.ofMillis(300)
        )
        Bukkit.getServer().showTitle(Title.title(Component.text(winnerMessage, winnerColor), scoreBarName(), tempos))
        Bukkit.getOnlinePlayers().forEach {
            it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        }

        val lobby = plugin.getSpawn("lobby")
        plugin.database.getLobbyPlayers().mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
            lobby?.let { player.teleport(it) }
            player.inventory.clear()
            player.clearActivePotionEffects()
            player.sendMessage(Component.text("The match has ended.", NamedTextColor.YELLOW))
            HubItem.give(plugin, player)
        }

        plugin.database.clearTeams()
        plugin.database.clearScores()
        plugin.matchRunning = false
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { this.tryStartLobbyCountdown() }, 20L * 5)
    }

    private fun startMatchClock() {
        this.stopMatchClock()
        scoreBar.color(BossBar.Color.WHITE)
        scoreBar.progress(1.0f)
        val start = System.currentTimeMillis()
        matchTimerTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable { this.endMatch() }, MATCH_DURATION_TICKS)
        clockTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!plugin.matchRunning) {
                this.stopMatchClock()
                return@Runnable
            }
            val elapsedTicks = (System.currentTimeMillis() - start) / 50L
            val remaining = (MATCH_DURATION_TICKS - elapsedTicks).coerceAtLeast(0L)
            scoreBar.progress(remaining.toFloat() / MATCH_DURATION_TICKS.toFloat())
            if (remaining <= 20L * 60L) scoreBar.color(BossBar.Color.RED)
        }, 0L, 20L)
    }

    private fun stopMatchClock() {
        matchTimerTask?.cancel()
        matchTimerTask = null
        clockTask?.cancel()
        clockTask = null
    }

    private fun startFreeze() {
        this.cancelFreeze()
        this.cancelCountdown()
        val anchors = this.onlineMatchPlayers()
            .associate { it.uniqueId to it.location.clone() }
        var ticks = 0L
        freezeTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            anchors.forEach { (uuid, anchor) ->
                val player = Bukkit.getPlayer(uuid) ?: return@forEach
                val loc = player.location
                if (loc.world != anchor.world || loc.distance(anchor) > 0.5) {
                    val back = anchor.clone()
                    back.yaw = loc.yaw
                    back.pitch = loc.pitch
                    player.teleport(back)
                    player.velocity = Vector(0.0, 0.0, 0.0)
                }
            }
            if (ticks == 60L) {
                this.cancelFreeze()
                teleportTeamsToSpawns()
                this.startGoalCountdown()
            }
            ticks += 2
        }, 0L, 2L)
    }

    fun cancelFreeze() {
        freezeTask?.cancel()
        freezeTask = null
    }

    private fun detonateBall(location: Location, team: Team) {
        location.world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1)
        location.world.spawn(location, Firework::class.java) { firework ->
            val meta = firework.fireworkMeta
            meta.addEffect(
                FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(teamColor(team))
                    .flicker(true)
                    .trail(true)
                    .build()
            )
            firework.fireworkMeta = meta
        }.detonate()
    }

    private fun startGoalCountdown() {
        this.cancelCountdown()
        var ticks = 0L
        var secondsLeft = 5
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            this.lockPlayersAtSpawns()
            if (ticks >= 20 && ticks <= 100 && ticks % 20 == 0L) {
                val pitch = 1.0f + (5 - secondsLeft) * 0.2f
                this.onlineMatchPlayers().forEach {
                    it.playSound(it.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch)
                }
                this.showCountdownTitle(Component.text(secondsLeft.toString(), NamedTextColor.YELLOW))
                secondsLeft--
            }
            if (ticks == 120L) {
                this.cancelCountdown()
                plugin.getSpawn("ball")?.let { spawnBall(it) }
                this.showCountdownTitle(Component.text("JOGAR!", NamedTextColor.GOLD))
                this.onlineMatchPlayers().forEach {
                    it.playSound(it.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f)
                }
            }
            ticks += 2
        }, 0L, 2L)
    }

    fun cancelCountdown() {
        countdownTask?.cancel()
        countdownTask = null
    }

    private fun onlineMatchPlayers(): List<Player> =
        Team.entries
            .flatMap { plugin.database.getTeamPlayers(it.name) }
            .mapNotNull { Bukkit.getPlayer(it) }

    fun hideScoreBar() {
        this.onlineMatchPlayers().forEach { it.hideBossBar(scoreBar) }
    }

    fun hideScoreBar(player: Player) {
        player.hideBossBar(scoreBar)
    }

    private fun scoreBarName(): Component {
        val red = plugin.database.getScore(Team.RED.name)
        val blue = plugin.database.getScore(Team.BLUE.name)
        return Component.text()
            .append(Component.text("RED $red", NamedTextColor.RED))
            .append(Component.text(" x ", NamedTextColor.GRAY))
            .append(Component.text("$blue BLUE", NamedTextColor.BLUE))
            .build()
    }

    private fun lockPlayersAtSpawns() {
        for (t in Team.entries) {
            val spawn = plugin.getSpawn(t.name.lowercase()) ?: continue
            plugin.database.getTeamPlayers(t.name)
                .mapNotNull { Bukkit.getPlayer(it) }
                .forEach { player ->
                    val loc = player.location
                    if (loc.world != spawn.world || loc.distance(spawn) > 0.5) {
                        val back = spawn.clone()
                        back.yaw = loc.yaw
                        back.pitch = loc.pitch
                        player.teleport(back)
                        player.velocity = Vector(0.0, 0.0, 0.0)
                    }
                }
        }
    }

    private fun numberTitle(text: Component): Title {
        val tempos = Title.Times.times(
            Duration.ofMillis(200),
            Duration.ofMillis(700),
            Duration.ofMillis(200)
        )
        return Title.title(text, Component.empty(), tempos)
    }

    private fun showCountdownTitle(text: Component) {
        Bukkit.getServer().showTitle(this.numberTitle(text))
    }

    private fun teleportTeamsToSpawns() {
        for (t in Team.entries) {
            val spawn = this.teamSpawnFor(t) ?: continue
            plugin.database.getTeamPlayers(t.name)
                .mapNotNull { Bukkit.getPlayer(it) }
                .forEach { it.teleport(spawn) }
        }
    }

    fun sendGoalMessage(team: Team) {
        var teamColor = NamedTextColor.RED
        var message = "Gol do time " + team.name
        if (team == Team.BLUE) {
            teamColor = NamedTextColor.BLUE
        }
        val tituloPrincipal = Component.text(message, teamColor)
        val subTitulo = scoreBarName()

        val tempos = Title.Times.times(
            Duration.ofMillis(300),
            Duration.ofMillis(700),
            Duration.ofMillis(300)
        )

        val titulo = Title.title(tituloPrincipal, subTitulo, tempos)
        Bukkit.getServer().showTitle(titulo)
    }

    fun preparePlayer(player: Player, team: Team) {
        this.setLookDirection(player, team)
        this.equipTeamArmor(player, team)
        this.applyPlayerEffects(player)
        PerkManager.giveAll(plugin, player)
        scoreBar.name(scoreBarName())
        player.showBossBar(scoreBar)
    }

    fun spawnBall(location: Location): SulfurCube {
        val world = location.world
        this.killTagged(ballTag)
        return world.spawn(location, SulfurCube::class.java) { ball ->
            ball.addScoreboardTag(ballTag)
            ball.size = 2
            ball.setAI(true)
            ball.swallow(ItemStack.of(Material.OAK_LOG))
        }
    }

    private fun killTagged(tag: String) {
        Bukkit.getWorlds().forEach { world ->
            world.entities
                .filter { it.scoreboardTags.contains(tag) }
                .forEach { it.remove() }
        }
    }

    private fun teamColor(team: Team): Color =
        if (team == Team.RED) Color.RED else Color.BLUE

    private fun equipTeamArmor(player: Player, team: Team) {
        // 1. Create the items
        val helmet = ItemStack(Material.LEATHER_HELMET)
        val chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
        val leggings = ItemStack(Material.LEATHER_LEGGINGS)
        val boots = ItemStack(Material.LEATHER_BOOTS)

        val color = teamColor(team)
        val armorKey = NamespacedKey(plugin, "match_armor")
        helmet.editMeta(LeatherArmorMeta::class.java) { meta ->
            meta.setColor(color)
            meta.persistentDataContainer.set(armorKey, PersistentDataType.BOOLEAN, true)
        }
        chestplate.editMeta(LeatherArmorMeta::class.java) { meta ->
            meta.setColor(color)
            meta.persistentDataContainer.set(armorKey, PersistentDataType.BOOLEAN, true)
        }
        leggings.editMeta(LeatherArmorMeta::class.java) { meta ->
            meta.setColor(color)
            meta.persistentDataContainer.set(armorKey, PersistentDataType.BOOLEAN, true)
        }
        boots.editMeta(LeatherArmorMeta::class.java) { meta ->
            meta.setColor(color)
            meta.persistentDataContainer.set(armorKey, PersistentDataType.BOOLEAN, true)
        }

        player.inventory.clear()
        player.inventory.setHelmet(helmet)
        player.inventory.setChestplate(chestplate)
        player.inventory.setLeggings(leggings)
        player.inventory.setBoots(boots)
    }

    private fun lookYaw(team: Team): Float =
        if (team == Team.RED) 270f else 90f // (0 is South, 90 is West, 180 is North, 270 is East)

    private fun teamSpawnFor(team: Team): Location? {
        val spawn = plugin.getSpawn(team.name.lowercase()) ?: return null
        return spawn.clone().apply {
            yaw = lookYaw(team)
            pitch = 0f
        }
    }

    private fun setLookDirection(player: Player, team: Team) {
        player.setRotation(lookYaw(team), 0f)
    }

    fun applyPlayerEffects(player: Player) {
        val duration = 600 * 100
        val effect = PotionEffect(PotionEffectType.SPEED, duration, 2)
        val effect2 = PotionEffect(PotionEffectType.SATURATION, duration, 5)
        player.addPotionEffect(effect)
        player.addPotionEffect(effect2)
    }

    companion object {
        private const val MATCH_DURATION_TICKS = 20L * 60 * 3
    }
}
