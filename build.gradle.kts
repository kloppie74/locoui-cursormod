import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    // 26.1 and newer ship unobfuscated: no mappings, no remapping.
    id("net.fabricmc.fabric-loom") version "1.19.3" apply false
    // 1.21.11 and older: Mojang's official mappings, remapped to intermediary for release.
    id("net.fabricmc.fabric-loom-remap") version "1.19.3" apply false
}

val modVersion = providers.gradleProperty("mod_version").get()
val loaderVersion = providers.gradleProperty("loader_version").get()

subprojects {
    val mc = name.removePrefix("mc-")
    val obfuscated = !mc.startsWith("26.")
    val javaVersion = property("java").toString().toInt()
    val glue = property("glue").toString()
    val mcRange = property("mc_range").toString()
    // The Minecraft versions this jar runs on, as players read them: locoui-cursor-1.0.0+1.21.6-1.21.8.jar
    val label = property("label").toString()

    apply(plugin = if (obfuscated) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")

    group = "app.phynx.locoui"
    version = "$modVersion+$label"
    extensions.configure<BasePluginExtension> { archivesName.set("locoui-cursor") }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
        withSourcesJar()
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(javaVersion)
        options.encoding = "UTF-8"
    }

    val loom = extensions.getByType<LoomGradleExtensionAPI>()
    dependencies {
        "minecraft"("com.mojang:minecraft:$mc")
        if (obfuscated) "mappings"(loom.officialMojangMappings())
        val implementation = if (obfuscated) "modImplementation" else "implementation"
        implementation("net.fabricmc:fabric-loader:$loaderVersion")
        implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api")}")
    }

    // The shared core plus this generation's glue (glue/<name>).
    extensions.configure<SourceSetContainer> {
        named("main") {
            java.srcDir(rootProject.file("common/src/main/java"))
            java.srcDir(rootProject.file("glue/$glue/java"))
            resources.srcDir(rootProject.file("common/src/main/resources"))
        }
    }

    tasks.named<ProcessResources>("processResources") {
        val props = mapOf("version" to version.toString(), "mc_range" to mcRange, "java" to javaVersion.toString())
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }
}
