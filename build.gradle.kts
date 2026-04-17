import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins{
    id("java")
    id("com.gradleup.shadow") version "9.3.1"
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.onarandombox.com/content/groups/public/") // Multiverse-Core repository
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT") {
        exclude( group = "org.yaml", module = "snakeyaml")
    }

    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.0.0-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveVersion.set("${project.properties.getValue("shaded_version")}")
        archiveClassifier.set("")

        minimize()
    }
}
tasks {
    build {
        dependsOn("shadowJar")
    }
}
tasks.compileJava {
    options.encoding = "UTF-8"
}

bukkit {
    prefix = "QuickVoid"
    name = rootProject.name
    main = "${rootProject.group}.quickvoid.QuickVoidPlugin"
    version = "${project.properties.getValue("shaded_version")}"
    description = "A plugin that allows admins to create void worlds without using a plugin implemented chunks generator."
    apiVersion = "1.13"
    authors = listOf("inc0g-repoz", "Aitooor")
    foliaSupported = true

    softDepend = listOf("Multiverse-Core")

    commands {
        register("quickvoid") {
            aliases = listOf("qv")
            description = "Reveals plugin functionality"
            permission = "quickvoid.admin"
            usage = "Usage: /<command> <create> <world_name>"
        }
    }

    permissions {
        register("quickvoid.admin") {
            description = "Allows you to use /quickvoid command"
        }
    }
}

paper {
    prefix = "QuickVoid"
    main = "${rootProject.group}.quickvoid.QuickVoidPlugin"
    bootstrapper = "${rootProject.group}.quickvoid.QuickVoidPluginBootstrap"
    apiVersion = "1.19"
    authors = listOf("inc0g-repoz", "Aitooor")
    foliaSupported = true

    serverDependencies {
        // During server run time, require LuckPerms, add it to the classpath, and load it before us
        register("LuckPMultiverse-Coreerms") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }

    permissions {
        register("quickvoid.admin") {
            description = "Allows you to use /quickvoid command"
        }
    }
}