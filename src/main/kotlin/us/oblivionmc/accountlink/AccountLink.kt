package us.oblivionmc.accountlink

import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import us.oblivionmc.accountlink.commands.LinkCommand
import us.oblivionmc.accountlink.commands.RefreshCommand
import us.oblivionmc.accountlink.commands.UnlinkCommand
import us.oblivionmc.accountlink.listeners.GroupUpdateListener
import java.io.File
import java.util.logging.Level


class AccountLink: JavaPlugin() {
    companion object {

       lateinit var instance: AccountLink
           private set
       lateinit var discordOAuthURL: String
           private set
       lateinit var unlinkURL: String
           private set
       lateinit var updateURL: String
           private set
       lateinit var updateKey: String
           private set
       lateinit var donorRanks: Map<String, String>
           private set

       var commandTimeout: Long = 0L

       lateinit var groupUpdateListener: GroupUpdateListener
           private set
    }
    override fun onEnable() {
        instance = this
        saveDefaultConfig()

        groupUpdateListener = GroupUpdateListener(instance, Bukkit.getServicesManager().getRegistration(LuckPerms::class.java).provider!!)

        val databaseDetails = config.getConfigurationSection("database").getValues(false)
        discordOAuthURL = config.getString("oauth-url")
        unlinkURL = config.getString("unlink-url")
        updateURL = config.getString("update-url")

        updateKey = config.getString("update-key")

        donorRanks = config.getConfigurationSection("donor-ranks").getValues(false).mapValues { return@mapValues it.value.toString() }
        for((k, v) in donorRanks) {
            logger.info("${k}: ${v}")
        }

        commandTimeout = config.getLong("command-timeout")

        if(databaseDetails.containsKey("host") && databaseDetails.containsKey("port") && databaseDetails.containsKey("database")
            && databaseDetails.containsKey("username") && databaseDetails.containsKey("password"))
        {
            if(databaseDetails["host"] is String && databaseDetails["port"] is Int && databaseDetails["database"] is String
                && databaseDetails["username"] is String && databaseDetails["password"] is String) {
                DatabaseManager.initializeDatabase(
                    databaseDetails["host"] as String, databaseDetails["port"] as Int, databaseDetails["database"] as String,
                    databaseDetails["username"] as String, databaseDetails["password"] as String
                )
            } else {
                logger.log(Level.SEVERE, """
                !!! =========================== !!!
                !!!    DATABASE ENTRIES ARE     !!!
                !!!   INCORRECTLY CONFIGURED    !!!
                !!! FOR DISCORD ACCOUNT LINKING !!!
                !!!  DISABLING UNTIL CONFIGURED !!!
                !!! =========================== !!!
            """.trimIndent())
                pluginLoader.disablePlugin(this)
                return
            }
        } else {
            logger.log(Level.SEVERE, """
                !!! ========================== !!!
                !!!    DATABASE ENTRIES ARE    !!!
                !!!    MISSING FOR DISCORD     !!!
                !!!      ACCOUNT LINKING       !!!       
                !!! DISABLING UNTIL CONFIGURED !!!
                !!! ========================== !!!
            """.trimIndent())
            File(dataFolder, "config.yml").delete()
            saveDefaultConfig()
            pluginLoader.disablePlugin(this)
            return
        }

        getCommand("link").executor = LinkCommand
        getCommand("unlink").executor = UnlinkCommand
        getCommand("refresh").executor = RefreshCommand

        logger.info("OblivionAccountLink has been enabled")
    }

    override fun onDisable() {
        DatabaseManager.deinitialize()
        TokenManager.deinitialize()
        logger.info("OblivionAccountLink has been disabled")
    }
}