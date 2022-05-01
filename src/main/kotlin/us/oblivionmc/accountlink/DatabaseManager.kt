package us.oblivionmc.accountlink

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.lang.Exception
import java.sql.Connection
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.javaType

object DatabaseManager {
    private lateinit var dataSource: HikariDataSource

    fun initializeDatabase(
        host: String,
        port: Int = 3306,
        database: String = "account_linking",
        dbusername: String,
        dbpassword: String,
        maxConnections: Int = 25
    ) {
        try {
            val config = HikariConfig()
            with(config) {
                jdbcUrl         = "jdbc:mysql://$host:$port/$database"
                driverClassName = "com.mysql.jdbc.Driver"
                username        = dbusername
                password        = dbpassword
                maximumPoolSize = maxConnections
            }

            dataSource = HikariDataSource(config)

            sendPreparedUpdateStatement("""
                CREATE TABLE IF NOT EXISTS `linked_accounts` (
                `discord_id` BIGINT UNSIGNED NOT NULL UNIQUE,
                `minecraft_uuid` BINARY(16) NOT NULL UNIQUE
                );
                """.trimIndent())
            sendPreparedUpdateStatement("DROP TABLE IF EXISTS `tokens`;")
            val tokenCreateReturn = sendPreparedUpdateStatement("""
                CREATE TABLE IF NOT EXISTS `tokens` (
                `token_id` BINARY(16) NOT NULL UNIQUE,
                `minecraft_uuid` BINARY(16) NOT NULL UNIQUE
                ) ENGINE = MEMORY;
                """.trimIndent())
            println("TokenCreateReturn: $tokenCreateReturn")
            sendPreparedUpdateStatement("DROP TABLE IF EXISTS `unlinktokens`;")
            val unlinkTokenCreateReturn = sendPreparedUpdateStatement("""
                CREATE TABLE IF NOT EXISTS `unlinktokens` (
                `token_id` BINARY(16) NOT NULL UNIQUE,
                `minecraft_uuid` BINARY(16) NOT NULL UNIQUE,
                `discord_id` BIGINT UNSIGNED NOT NULL UNIQUE
                ) ENGINE = MEMORY;
                """.trimIndent())
            println("UnlinkTokenCreateReturn: $unlinkTokenCreateReturn")
        } catch (e: Exception) {
            when (e) {
                is InterruptedException, is ExecutionException, is CancellationException -> {
                    AccountLink.instance.logger.severe("An exception has occurred while attempting to connect to the database")
                    println(e)
                    AccountLink.instance.logger.severe(
                        """
                            !!! ========================== !!!
                            !!!    DISCORD LINK IS NOT     !!!
                            !!!    CORRECTLY CONFIGURED    !!!
                            !!! DISABLING UNTIL CONFIGURED !!!
                            !!! ========================== !!!
                        """.trimIndent()
                    )
                    AccountLink.instance.pluginLoader.disablePlugin(AccountLink.instance)
                    return
                }
                else -> {
                    AccountLink.instance.logger.severe("An exception has occurred")
                    println(e)
                    AccountLink.instance.logger.severe(
                        """
                            !!! ========================== !!!
                            !!!    DISCORD LINK IS NOT     !!!
                            !!!    CORRECTLY CONFIGURED    !!!
                            !!! DISABLING UNTIL CONFIGURED !!!
                            !!! ========================== !!!
                        """.trimIndent()
                    )
                    AccountLink.instance.pluginLoader.disablePlugin(AccountLink.instance)
                    return
                }
            }
        }
    }

    fun deinitialize() {
        sendPreparedUpdateStatement("DROP TABLE `tokens`;")
        dataSource.close()
    }

    inline fun <reified T : Any> sendPreparedQueryStatement(query: String) = sendPreparedQueryStatement<T>(query, listOf(), 30)
    inline fun <reified T : Any> sendPreparedQueryStatement(query: String, timeout: Int) = sendPreparedQueryStatement<T>(query, listOf(), timeout)
    inline fun <reified T : Any> sendPreparedQueryStatement(query: String, values: List<Any?>) = sendPreparedQueryStatement<T>(query, values, 30)
    inline fun <reified T : Any> sendPreparedQueryStatement(query: String, values: List<Any?>, timeout: Int) = sendPreparedQueryStatement(T::class, query, values, timeout)
    fun <T : Any> sendPreparedQueryStatement(c: KClass<T>, query: String, values: List<Any?>, timeout: Int): List<T> {
        var output = mutableListOf<T>()
        var con: Connection? = null;
        try {
            con = dataSource.connection
            val pst = con.prepareStatement(query)
            if(values.count() > 0) {
                var index = 0
                for (v in values) {
                    index += 1
                    pst.setObject(index, v)
                }
            }
            pst.queryTimeout = timeout
            val sqlReturn = pst.executeQuery()
            while(sqlReturn.next()) {
                val instance = c.createInstance()
                c.members
                    .filter { it.visibility == KVisibility.PUBLIC }
                    .filterIsInstance<KMutableProperty<*>>()
                    .forEach { prop -> prop.setter.call(instance, sqlReturn.getObject(prop.name)) }
                output.add(instance)
            }
            con.close()
            return output
        } catch (e: Exception) {
            println(e)
            throw e
        } finally {
            con?.close()
        }
    }

    fun sendPreparedUpdateStatement(query: String) = sendPreparedUpdateStatement(query, listOf(), 30)
    fun sendPreparedUpdateStatement(query: String, timeout: Int) = sendPreparedUpdateStatement(query, listOf(), timeout)
    fun sendPreparedUpdateStatement(query: String, values: List<Any?>) = sendPreparedUpdateStatement(query, values, 30)
    fun sendPreparedUpdateStatement(query: String, values: List<Any?>, timeout: Int): Int? { //Error java.sql.SQLException: Invalid argument value: java.io.NotSerializableException
        var con: Connection? = null;
        try {
            con = dataSource.connection
            val pst = con.prepareStatement(query)
            if(values.count() > 0) {
                var index = 0
                for (v in values) {
                    index += 1
                    println("$index: ${v.toString()}")
                    pst.setObject(index, v)
                }
            }
            pst.queryTimeout = timeout
            println(pst.toString())
            val sqlReturn = pst.executeUpdate()
            con.close()
            return sqlReturn
        } catch (e: Exception) {
            println(e)
            return null
        } finally {
            con?.close()
        }
    }
}