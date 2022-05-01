package us.oblivionmc.accountlink.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import us.oblivionmc.accountlink.AccountLink
import us.oblivionmc.accountlink.DatabaseManager
import us.oblivionmc.accountlink.TokenManager
import us.oblivionmc.accountlink.TokenManager.asBytes
import java.time.LocalDateTime
import java.time.ZoneOffset
import us.oblivionmc.accountlink.data.*

object UnlinkCommand : CommandExecutor {

    private var timeoutTimes = hashMapOf<Player, Long>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            AccountLink.instance.server.scheduler.runTaskAsynchronously(AccountLink.instance)
            {
                if (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > timeoutTimes[sender] ?: 0) {
                    val linkedAccountResult = DatabaseManager.sendPreparedQueryStatement<LinkedAccountEntry>(
                        "SELECT * FROM `linked_accounts` WHERE `minecraft_uuid` = ?",
                        listOf(sender.uniqueId.asBytes())
                    )
                    if (linkedAccountResult.isNotEmpty()) {
                        val unlinkToken =
                            TokenManager.createUnlinkToken(sender.uniqueId, linkedAccountResult[0].discord_id.toLong().toULong())
                        sender.sendMessage(
                            *ComponentBuilder("[Click Here]")
                                .color(ChatColor.RED)
                                .bold(true)
                                .event(
                                    ClickEvent(
                                        ClickEvent.Action.OPEN_URL,
                                        "${AccountLink.unlinkURL}?unlinkToken=${unlinkToken.token}"
                                    )
                                )
                                .append(" to unlink your Discord account.", ComponentBuilder.FormatRetention.NONE)
                                .color(ChatColor.RED)
                                .create()
                        )
                    } else {
                        sender.sendMessage(
                            *ComponentBuilder("You need to link via /link before you can unlink a Discord account")
                                .color(ChatColor.RED)
                                .bold(true)
                                .create()
                        )
                    }
                    timeoutTimes[sender] =
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + AccountLink.commandTimeout
                } else {
                    sender.sendMessage(
                        *ComponentBuilder("You need to wait to run this command again")
                            .color(ChatColor.RED)
                            .bold(true)
                            .create()
                    )
                }
            }
            return true
        } else {
            sender.sendMessage("This command can only be run by a player")
            return false
        }

    }
}