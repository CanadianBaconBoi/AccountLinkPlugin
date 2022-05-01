package us.oblivionmc.accountlink

import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import us.oblivionmc.accountlink.data.*
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

object TokenManager {
    private const val EXPIRE_TIME = 1800L // 1800 seconds = 30 minutes
    private val tokens: MutableMap<UUID, Token> = mutableMapOf()
    private val unlinkTokens: MutableMap<UUID, UnlinkToken> = mutableMapOf()

    data class Token(val token: UUID, val playerUuid: UUID, val diesOn: Long)
    data class UnlinkToken(val token: UUID, val playerUuid: UUID, val discordId: ULong, val diesOn: Long)

    private val expiredTokens = mutableListOf<UUID>()
    private var now: Long = 0L

    private class Runnable: BukkitRunnable() {
        override fun run() {
            if (tokens.count() > 0) {
                now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                for (token in tokens)
                    if (token.value.diesOn <= now)
                        expiredTokens.add(token.key)
                if(expiredTokens.size > 0) {
                    val statement = StringBuilder("DELETE FROM `tokens` WHERE `minecraft_uuid` in (")
                    for (token in expiredTokens) {
                        statement.append("?,")
                        tokens.remove(token)
                    }
                    statement[statement.length - 1] = ')'
                    statement.append(';')

                    DatabaseManager.sendPreparedUpdateStatement(statement.toString(), expiredTokens.asBytes())
                    expiredTokens.clear()
                }
            }
        }
    }

    private var runTask: BukkitTask =
        Runnable().runTaskTimerAsynchronously(AccountLink.instance, 20*EXPIRE_TIME, 20*EXPIRE_TIME)

    fun createToken(playerUuid: UUID): Token {
        val queryResult = DatabaseManager.sendPreparedQueryStatement<TokenEntry>("SELECT * FROM `tokens` WHERE `minecraft_uuid` = ?", listOf(playerUuid.asBytes()));
        if(queryResult.isEmpty()) {
            if(tokens.containsKey(playerUuid)) {
                tokens.remove(playerUuid)
            }
            val tokenUuid = UUID.randomUUID()
            val token = Token(tokenUuid, playerUuid, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + EXPIRE_TIME)
            DatabaseManager.sendPreparedUpdateStatement("INSERT INTO `tokens` (`token_id`, `minecraft_uuid`) VALUES (?, ?)", listOf(tokenUuid.asBytes(), playerUuid.asBytes()))
            tokens[playerUuid] = token
            return token
        } else {
            if(tokens.containsKey(playerUuid)) {
                return tokens[playerUuid]!!
            }
            val token = Token(UUID.nameUUIDFromBytes(queryResult[0].token_id), playerUuid, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + EXPIRE_TIME)
            tokens[playerUuid] = token
            return token
        }
    }

    fun createUnlinkToken(playerUuid: UUID, discordId: ULong): UnlinkToken {
        val queryResult = DatabaseManager.sendPreparedQueryStatement<UnlinkTokenEntry>("SELECT * FROM `unlinktokens` WHERE `minecraft_uuid` = ?", listOf(playerUuid.asBytes()));
        if(queryResult.isEmpty()) {
            if(unlinkTokens.containsKey(playerUuid)) {
                unlinkTokens.remove(playerUuid)
            }
            val unlinkTokenUuid = UUID.randomUUID()
            val unlinkToken = UnlinkToken(unlinkTokenUuid, playerUuid, discordId, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + EXPIRE_TIME)
            DatabaseManager.sendPreparedUpdateStatement("INSERT INTO `unlinktokens` (`token_id`, `minecraft_uuid`, `discord_id`) VALUES (?, ?, ?)", listOf(unlinkTokenUuid.asBytes(), playerUuid.asBytes(), discordId.toLong()))
            unlinkTokens[playerUuid] = unlinkToken
            return unlinkToken
        } else {
            if(unlinkTokens.containsKey(playerUuid)) {
                return unlinkTokens[playerUuid]!!
            }
            val unlinkToken = UnlinkToken(UUID.nameUUIDFromBytes(queryResult[0].token_id), playerUuid, discordId, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + EXPIRE_TIME)
            unlinkTokens[playerUuid] = unlinkToken
            return unlinkToken
        }
    }

    fun removePlayerTokenIfExists(playerUuid: UUID): Token? {
        return tokens.remove(playerUuid)
    }

    fun deinitialize() {
        runTask.cancel()
    }

    public fun UUID.asBytes(): ByteArray {
        val b = ByteBuffer.wrap(ByteArray(16))
        b.putLong(mostSignificantBits)
        b.putLong(leastSignificantBits)
        return b.array()
    }

    public fun List<UUID>.asBytes(): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        for(uuid in this) {
            list.add(uuid.asBytes())
        }
        return list
    }
}