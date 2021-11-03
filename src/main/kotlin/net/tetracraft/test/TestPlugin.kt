package net.tetracraft.test

import org.bukkit.plugin.java.JavaPlugin

class TestPlugin: JavaPlugin() {
    override fun onEnable() {
        server.logger.info("Hello from TetraTestPlugin")
        super.onEnable()
    }

    override fun onDisable() {
        server.logger.info("Goodbye from TetraTestPlugin")
        super.onDisable()
    }
}