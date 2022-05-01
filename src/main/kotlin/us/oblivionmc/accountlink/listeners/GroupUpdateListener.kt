package us.oblivionmc.accountlink.listeners

import net.luckperms.api.LuckPerms
import net.luckperms.api.event.node.NodeAddEvent
import net.luckperms.api.event.node.NodeMutateEvent
import net.luckperms.api.event.node.NodeRemoveEvent
import net.luckperms.api.model.user.User
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import us.oblivionmc.accountlink.AccountLink
import java.io.IOException
import java.util.prefs.NodeChangeEvent


class GroupUpdateListener(plugin: AccountLink, luckperms: LuckPerms) {
    val client = OkHttpClient()

    init {
        var eventBus = luckperms.eventBus;

        eventBus.subscribe(plugin, NodeAddEvent::class.java, this::onNodeAdd)
        eventBus.subscribe(plugin, NodeRemoveEvent::class.java, this::onNodeRemove)
    }

    private fun onNodeAdd(event: NodeAddEvent) {
        AccountLink.instance.logger.info("Node Add Event")
        AccountLink.instance.logger.info("Target is User: ${event.target is User} | Node: ${event.node.key}, Shortened: ${event.node.key.substring(6)}, Value: ${event.node.value}")
        if (event.target is User && event.node.key.startsWith("group.")
            && AccountLink.donorRanks.containsKey(event.node.key.substring(6))
        ) {
            AccountLink.instance.logger.info("Node Add Event Start")
            val body = FormBody.Builder()
                .add("target", (event.target as User).uniqueId.toString())
                .add("group", event.node.key.substring(6))
                .add("action", if(event.node.value) "add" else "remove")
                .build()
            val req = Request.Builder()
                .url(AccountLink.updateURL)
                .addHeader("Authorization", "Bearer ${AccountLink.updateKey}")
                .addHeader("User-Agent", "Oblivion AccountLink Plugin")
                .post(body)
                .build()
            var response: Response? = null;
            try {
                response = client.newCall(req).execute()
                if(!response.isSuccessful) throw IOException("Unexpected code: ${response.code}")
            } catch (e: IOException) {
                AccountLink.instance.logger.severe("Unable to update user rank")
                AccountLink.instance.logger.severe(e.message)
                throw e
            } finally {
                response?.close()
            }
        }
    }

    private fun onNodeRemove(event: NodeRemoveEvent) {
        AccountLink.instance.logger.info("Node Remove Event")
        if (event.isUser && event.node.key.startsWith("group.")
            && AccountLink.donorRanks.containsKey(event.node.key.substring(6))
        ) {
            AccountLink.instance.logger.info("Node Remove Start")
            val body = FormBody.Builder()
                .add("target", (event.target as User).uniqueId.toString())
                .add("group", event.node.key.substring(6))
                .add("action", if(event.node.value) "remove" else "add")
                .build()
            val req = Request.Builder()
                .url(AccountLink.updateURL)
                .addHeader("Authorization", "Bearer ${AccountLink.updateKey}")
                .addHeader("User-Agent", "Oblivion AccountLink Plugin")
                .post(body)
                .build()
            var response: Response? = null;
            try {
                response = client.newCall(req).execute()
                if(!response.isSuccessful) throw IOException("Unexpected code: ${response.code}")
            } catch (e: IOException) {
                AccountLink.instance.logger.severe("Unable to update user rank")
                AccountLink.instance.logger.severe(e.message)
                throw e
            } finally {
                response?.close()
            }
        }
    }
}