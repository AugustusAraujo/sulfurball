package org.gustin.sulfurBall

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class Database(plugin: JavaPlugin) {

    private val connection: Connection

    init {
        plugin.dataFolder.mkdirs()
        val file = File(plugin.dataFolder, "data.db")
        connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")

        connection.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS lobby_players (
                    uuid      TEXT PRIMARY KEY,
                    joined_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS match_team (
                    uuid TEXT PRIMARY KEY,
                    team TEXT NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS team_score (
                    team  TEXT PRIMARY KEY,
                    score INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS player_perk (
                    uuid TEXT PRIMARY KEY,
                    perk  TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Synchronized
    fun addLobbyPlayer(uuid: UUID) {
        connection.prepareStatement(
            "INSERT OR IGNORE INTO lobby_players (uuid, joined_at) VALUES (?, ?)"
        ).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.setLong(2, System.currentTimeMillis())
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun removeLobbyPlayer(uuid: UUID) {
        connection.prepareStatement("DELETE FROM lobby_players WHERE uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun getLobbyPlayers(): List<UUID> {
        connection.createStatement().use { st ->
            st.executeQuery("SELECT uuid FROM lobby_players ORDER BY joined_at").use { rs ->
                val list = mutableListOf<UUID>()
                while (rs.next()) list += UUID.fromString(rs.getString("uuid"))
                return list
            }
        }
    }

    @Synchronized
    fun clearLobby() {
        connection.createStatement().use { it.executeUpdate("DELETE FROM lobby_players") }
    }

    @Synchronized
    fun setTeam(uuid: UUID, team: String) {
        connection.prepareStatement(
            """
            INSERT INTO match_team (uuid, team) VALUES (?, ?)
            ON CONFLICT(uuid) DO UPDATE SET team = excluded.team
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.setString(2, team)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun getTeam(uuid: UUID): String? {
        connection.prepareStatement("SELECT team FROM match_team WHERE uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeQuery().use { rs ->
                return if (rs.next()) rs.getString("team") else null
            }
        }
    }

    @Synchronized
    fun removeFromTeam(uuid: UUID) {
        connection.prepareStatement("DELETE FROM match_team WHERE uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun getTeamPlayers(team: String): List<UUID> {
        connection.prepareStatement("SELECT uuid FROM match_team WHERE team = ?").use { stmt ->
            stmt.setString(1, team)
            stmt.executeQuery().use { rs ->
                val list = mutableListOf<UUID>()
                while (rs.next()) list += UUID.fromString(rs.getString("uuid"))
                return list
            }
        }
    }

    @Synchronized
    fun addPoint(team: String) {
        connection.prepareStatement(
            """
            INSERT INTO team_score (team, score) VALUES (?, 1)
            ON CONFLICT(team) DO UPDATE SET score = score + 1
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, team)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun getScore(team: String): Int {
        connection.prepareStatement("SELECT score FROM team_score WHERE team = ?").use { stmt ->
            stmt.setString(1, team)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) rs.getInt("score") else 0
            }
        }
    }

    @Synchronized
    fun clearScores() {
        connection.createStatement().use { it.executeUpdate("DELETE FROM team_score") }
    }

    @Synchronized
    fun setSelectedPerk(uuid: UUID, perk: String) {
        connection.prepareStatement(
            """
            INSERT INTO player_perk (uuid, perk) VALUES (?, ?)
            ON CONFLICT(uuid) DO UPDATE SET perk = excluded.perk
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.setString(2, perk)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun getSelectedPerk(uuid: UUID): String? {
        connection.prepareStatement("SELECT perk FROM player_perk WHERE uuid = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeQuery().use { rs ->
                return if (rs.next()) rs.getString("perk") else null
            }
        }
    }

    @Synchronized
    fun clearTeams() {
        connection.createStatement().use { it.executeUpdate("DELETE FROM match_team") }
    }

    fun close() = connection.close()
}