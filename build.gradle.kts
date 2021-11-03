import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.*

plugins {
    id("java")
    id("maven-publish")
    kotlin("jvm") version "1.5.31"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.0"
}

repositories {
    maven {
        name = "local-maven-central"
        url = uri("http://server1:8081/repository/maven-public/")
        credentials {
            username = "admin"
            password = "^aqVKj8Yo7Sx2^hvfnNd0S2CV!OUJk"
        }
        isAllowInsecureProtocol = true
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.google.code.gson", "gson", "2.8.7") // All platforms
}

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:1.29")
    }
    repositories {
        maven {
            name = "local-maven-central"
            url = uri("http://server1:8081/repository/maven-public/")
            credentials {
                username = "admin"
                password = "^aqVKj8Yo7Sx2^hvfnNd0S2CV!OUJk"
            }
            isAllowInsecureProtocol = true
        }
    }
}

val targetJavaVersion = 16
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType(JavaCompile::class).configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = rootProject.name
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(rootProject.name)
                description.set(rootProject.description)
                url.set(rootProject.properties["url"] as String)
                developers {
                    developer {
                        id.set("CanadianBacon")
                        name.set("Connor Beam")
                        email.set("beamconnor@gmail.com")
                    }
                    developer {
                        id.set("KoromaruKoruko")
                        name.set("Bailey Drahoss")
                        email.set("bailey.drahoss@outlook.com")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = "http://server1.fritz.box:8081/repository/maven-releases/"
            val snapshotsRepoUrl = "http://server1:8081/repository/maven-snapshots/"
            url = uri(if((version as String).endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = "admin"
                password = "^aqVKj8Yo7Sx2^hvfnNd0S2CV!OUJk"
            }
            isAllowInsecureProtocol = true
        }
    }
}

// Custom parser below for creating plugin.yml leveraging Minecrell's plugin.yml generator
// Most configuration entries can be set in gradle.properties

// Commands and Permissions are setup in the "bukkit" directory in their respective "commands" and "permissions" subdirectories
// example.yaml files are included in both directories

// Example gradle.properties below (This will generate a working plugin.yml)
/*

name = Test Plugin
version = 1.0-SNAPSHOT
description = Plugin for testing
author = Notch (Markus Pearson)
url = https://www.minecraft.net

spigotMainClass = com.test.TestPlugin
# One of 1.12, 1.13, 1.14, 1.15, or 1.16
spigotApiVersion = 1.16
# In format Dependency1,Dependency2,Dependency3
spigotDependencies = WorldEdit
# In format TestPlugin,TestPluginOldName
spigotProvides = TestPlugin,TestPluginOldName
spigotLogPrefix = TEST
# One of TRUE, FALSE, OP, or NOT_OP
defaultPermission = OP
# STARTUP or POSTWORLD
pluginLoadOrder = POSTWORLD

group = com.test

*/

//DO NOT TOUCH ANYTHING PAST THIS POINT
//DO NOT TOUCH ANYTHING PAST THIS POINT
//DO NOT TOUCH ANYTHING PAST THIS POINT
//DO NOT TOUCH ANYTHING PAST THIS POINT

bukkit {
    name = rootProject.properties["name"] as String
    version = rootProject.properties["version"] as String
    main = rootProject.properties["spigotMainClass"] as String
    apiVersion = rootProject.properties.getOrDefault("spigotApiVersion", null) as String?
    load = (rootProject.properties.getOrDefault("pluginLoadOrder", null) as String?)?.let { PluginLoadOrder.valueOf(it) }
    depend = (rootProject.properties.getOrDefault("spigotDependencies", null) as String?)?.split(",")
    prefix = rootProject.properties.getOrDefault("spigotLogPrefix", null) as String?
    defaultPermission = (rootProject.properties.getOrDefault("defaultPermission", null) as String?)?.let { Permission.Default.valueOf(it) }
    provides = (rootProject.properties.getOrDefault("spigotProvides", null) as String?)?.split(",")
    authors = (rootProject.properties.getOrDefault("authors", null) as String?)?.split(",")
    website = rootProject.properties.getOrDefault("url", null) as String?
    description = rootProject.properties.getOrDefault("description", null) as String?
    libraries = (rootProject.properties.getOrDefault("spigotLibraries", null) as String?)?.split(",")
    softDepend = (rootProject.properties.getOrDefault("softDepends", null) as String?)?.split(",")
    loadBefore = (rootProject.properties.getOrDefault("loadBefore", null) as String?)?.split(",")

    val yaml = org.yaml.snakeyaml.Yaml()

    commands {
        File("bukkit/commands/").walk().filter{ it.extension == "yaml" || it.extension == "yml" }.forEach {
            val yamlData: Map<String, Any> = yaml.load(it.inputStream())
            val rootPermissionNode = yamlData["root-permission-node"] as String
            val iter = (yamlData["commands"] as Map<String, Any>).iterator()
            while(iter.hasNext()) {
                val command = iter.next()
                val commandData = command.value as Map<String, Any>
                register(command.key) {
                    description = commandData.getOrDefault("description", null) as String?
                    aliases = commandData.getOrDefault("aliases", null) as ArrayList<String>?
                    if(commandData.containsKey("permission")) {
                        if((commandData["permission"] as String).startsWith("..")) {
                            permission = rootPermissionNode + (commandData["permission"] as String).substring(1)
                        } else {
                            permission = commandData["permission"] as String
                        }
                    } else {
                        throw GradleException("Command ${command.key} in file ${it.name} is missing it's permission")
                    }
                    permissionMessage = commandData.getOrDefault("permission-message", null) as String?
                    usage = commandData.getOrDefault("usage", null) as String?
                }
            }
        }
    }

    permissions {
        File("bukkit/permissions/").walk().filter{ it.extension == "yaml" || it.extension == "yml" }.forEach { file ->
            val yamlData: Map<String, Any> = yaml.load(file.inputStream())
            val rootPermissionNode = yamlData["root-permission-node"] as String
            val iter = (yamlData["permissions"] as Map<String, Any>).iterator()
            while(iter.hasNext()) {
                val permission = iter.next()
                val permissionData = permission.value as Map<String, Any>
                register(if(permission.key.startsWith(".")) rootPermissionNode+permission.key else permission.key) {
                    description = permissionData.getOrDefault("description", null) as String?
                    default = when(permissionData.getOrDefault("default", null)) {
                        "op", "OP", "admin", "ADMIN" -> Permission.Default.OP
                        "!op", "!OP", "notop", "NOTOP", "not_op", "NOT_OP", "!admin", "!ADMIN" -> Permission.Default.NOT_OP
                        "true", "TRUE", "True" -> Permission.Default.TRUE
                        "false", "FALSE", "False" -> Permission.Default.FALSE
                        else -> null
                    }
                    if(permissionData.getOrDefault("children", null) != null) {
                        val childrenIterator = (permissionData["children"] as Map<String, *>).iterator()
                        val childMap = HashMap<String, Boolean>()
                        childrenIterator.forEach {
                            childMap[if(it.key.startsWith(".")) rootPermissionNode+it.key else it.key] =
                                (if(it.value is Boolean) it.value
                                else throw GradleException("Misconfigured child permissions of ${permission.key} in file ${file.name}")) as Boolean
                        }
                        childrenMap = childMap
                    }
                }
            }
        }
    }
}
