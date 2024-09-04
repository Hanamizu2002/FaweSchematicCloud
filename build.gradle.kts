import io.papermc.hangarpublishplugin.model.Platforms
import org.ajoberstar.grgit.Grgit
import xyz.jpenilla.runpaper.task.RunServer
import java.util.*

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("com.modrinth.minotaur") version "2.8.7"
    id("org.ajoberstar.grgit") version "5.2.1"
    id("org.jetbrains.changelog") version "2.2.0"
}

if (!File("$rootDir/.git").exists()) {
    logger.lifecycle(
        """
    **************************************************************************************
    You need to fork and clone this repository! Don't download a .zip file.
    If you need assistance, consult the GitHub docs: https://docs.github.com/get-started/quickstart/fork-a-repo
    **************************************************************************************
    """.trimIndent()
    ).also { System.exit(1) }
}

var baseVersion by extra("1.3.0")
var extension by extra("")
var snapshot by extra("-SNAPSHOT")

group = "dev.themeinerlp"

ext {
    val git: Grgit = Grgit.open {
        dir = File("$rootDir/.git")
    }
    val revision = git.head().abbreviatedId
    extension = "%s+%s".format(Locale.ROOT, snapshot, revision)
}


version = "%s%s".format(Locale.ROOT, baseVersion, extension)

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    // Paper
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    // Fawe Support
    implementation(platform("com.intellectualsites.bom:bom-newest:1.48"))
    implementation("com.intellectualsites.arkitektonika:Arkitektonika-Client:2.1.3")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") {
        isTransitive = false
    }
    implementation("me.lucko:commodore:2.2")

    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

val supportedMinecraftVersions = listOf(
    "1.21"
)

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    supportedMinecraftVersions.forEach { serverVersion ->
        register<RunServer>("run-$serverVersion") {
            minecraftVersion(serverVersion)
            jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
            group = "run paper"
            runDirectory.set(file("run-$serverVersion"))
            pluginJars(rootProject.tasks.shadowJar.map { it.archiveFile }.get())
        }
    }
    register<RunServer>("runFolia") {
        downloadsApiService.set(xyz.jpenilla.runtask.service.DownloadsAPIService.folia(project))
        minecraftVersion("1.21")
        group = "run paper"
        runDirectory.set(file("run-folia"))
        jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
    }
    generateBukkitPluginDescription {
        doLast {
            outputDirectory.file(fileName).get().asFile.appendText("folia-supported: true")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

bukkit {
    main = "dev.themeinerlp.faweschematiccloud.FAWESchematicCloud"
    apiVersion = "1.21"
    authors = listOf("TheMeinerLP")
    depend = listOf("FastAsyncWorldEdit")
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
