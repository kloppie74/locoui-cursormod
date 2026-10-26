import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    // 26.1 and newer ship unobfuscated: no mappings, no remapping.
    id("net.fabricmc.fabric-loom") version "1.19.3" apply false
    // 1.21.11 and older: Mojang's official mappings, remapped to intermediary for release.
    id("net.fabricmc.fabric-loom-remap") version "1.19.3" apply false
}

val modVersion = providers.gradleProperty("mod_version").get()
val loaderVersion = providers.gradleProperty("loader_version").get()
val universalRange = providers.gradleProperty("universal_range").get()

subprojects {
    val mc = name.removePrefix("mc-")
    val obfuscated = !mc.startsWith("26.")
    val javaVersion = property("java").toString().toInt()
    val glue = property("glue").toString()
    val mcRange = property("mc_range").toString()
    // The Minecraft versions this jar runs on, as players read them: locoui-cursor-1.1.0+1.21.6-1.21.8.jar
    val label = property("label").toString()
    // This build's own glue package (glue.v1_21_6_1_21_8): the glue sources say `glue.current`,
    // and two builds of the same sources (26.2 and 26.3 differ in Minecraft, not in source)
    // must not collide in the universal jar.
    val glueId = "v" + label.replace(Regex("[^0-9]+"), "_")
    // The mouse mixin, compiled for the names the game runs on.
    val family = if (obfuscated) "intermediary" else "mojang"
    extra["obfuscated"] = obfuscated

    apply(plugin = if (obfuscated) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")

    group = "app.phynx.locoui"
    version = "$modVersion+$label"
    extensions.configure<BasePluginExtension> { archivesName.set("locoui-cursor") }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
        withSourcesJar()
    }
    tasks.withType<JavaCompile>().configureEach {
        // Java 21 bytecode everywhere, also where the game itself needs Java 25: the universal
        // jar's mixin config lists the 26.x mixin on 1.21.x too, and Mixin reads every listed class.
        options.release.set(21)
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

    val generateGlue = tasks.register<Sync>("generateGlue") {
        into(layout.buildDirectory.dir("generated/glue/java"))
        from(rootProject.file("src/glue/$glue/app/phynx/locoui/cursormod/glue/current")) {
            into("app/phynx/locoui/cursormod/glue/$glueId")
            filter { line: String -> line.replace("app.phynx.locoui.cursormod.glue.current", "app.phynx.locoui.cursormod.glue.$glueId") }
        }
        from(rootProject.file("src/common/mixin/app/phynx/locoui/cursormod/mixin/family")) {
            into("app/phynx/locoui/cursormod/mixin/$family")
            filter { line: String -> line.replace("app.phynx.locoui.cursormod.mixin.family", "app.phynx.locoui.cursormod.mixin.$family") }
        }
    }
    // One line for Versions.glue(): "<glue> <Minecraft versions>".
    val generateGlueTable = tasks.register("generateGlueTable") {
        val dir = layout.buildDirectory.dir("generated/glue/resources")
        val row = "$glueId $mcRange"
        inputs.property("row", row)
        outputs.dir(dir)
        doLast { dir.get().file("locoui_cursor_glues.txt").asFile.writeText(row + "\n") }
    }

    // The shared core plus this generation's glue (glue/<name>) in its own package.
    extensions.configure<SourceSetContainer> {
        named("main") {
            java.srcDir(rootProject.file("src/common/java"))
            java.srcDir(generateGlue)
            resources.srcDir(rootProject.file("src/common/resources"))
            resources.srcDir(generateGlueTable)
        }
    }

    tasks.named<ProcessResources>("processResources") {
        val props = mapOf(
            "version" to version.toString(), "mc_range" to mcRange, "java" to javaVersion.toString(),
            "client_mixins" to "\"$family.PlayerTurnMixin\"",
        )
        inputs.properties(props)
        filesMatching(listOf("fabric.mod.json", "locoui_cursor.mixins.json")) { expand(props) }
    }
}

// ONE JAR FOR EVERY VERSION: the shared classes once (from the 1.21.4 build: Java 21
// bytecode), every build's glue package, both turn mixins, and a glue table with all
// rows. At start the mod picks the glue and the mixin for the running game. The jar lands
// in versions/<mod version>/: the history of every released jar.
val universalBase = ":mc-1.21.4"

fun finalJar(p: Project) = p.tasks.named<AbstractArchiveTask>(if (p.extra["obfuscated"] as Boolean) "remapJar" else "jar")

val universalMeta = tasks.register<Sync>("universalMeta") {
    into(layout.buildDirectory.dir("generated/universal/meta"))
    from("src/common/resources") {
        include("fabric.mod.json", "locoui_cursor.mixins.json")
        expand(mapOf(
            "version" to modVersion, "mc_range" to universalRange, "java" to "21",
            "client_mixins" to "\"intermediary.PlayerTurnMixin\", \"mojang.PlayerTurnMixin\"",
        ))
    }
}

val universalGlueTable = tasks.register("universalGlueTable") {
    val dir = layout.buildDirectory.dir("generated/universal/table")
    val tables = subprojects.sortedBy { it.name }.map { it.layout.buildDirectory.file("generated/glue/resources/locoui_cursor_glues.txt") }
    dependsOn(subprojects.map { it.tasks.named("generateGlueTable") })
    inputs.files(tables)
    outputs.dir(dir)
    doLast { dir.get().file("locoui_cursor_glues.txt").asFile.writeText(tables.joinToString("") { it.get().asFile.readText() }) }
}

val universalJar = tasks.register<Jar>("universalJar") {
    group = "build"
    description = "Builds locoui-cursor-<version>.jar, one jar for every supported Minecraft version."
    archiveBaseName.set("locoui-cursor")
    archiveVersion.set(modVersion)
    destinationDirectory.set(layout.projectDirectory.dir("versions/$modVersion"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes("Implementation-Title" to "LocoUI Cursor", "Implementation-Version" to modVersion) }
    val base = project(universalBase)
    for (p in listOf(base) + subprojects.filter { it != base }.sortedBy { it.name }) {
        val archive = finalJar(p)
        dependsOn(archive)
        from(archive.flatMap { it.archiveFile }.map { zipTree(it) }) {
            if (p == base) {
                exclude("fabric.mod.json", "locoui_cursor.mixins.json", "locoui_cursor_glues.txt", "META-INF/**")
            } else {
                include("app/phynx/locoui/cursormod/glue/**", "app/phynx/locoui/cursormod/mixin/intermediary/**", "app/phynx/locoui/cursormod/mixin/mojang/**")
            }
        }
    }
    from(universalMeta)
    from(universalGlueTable)
}

tasks.register("build") {
    group = "build"
    description = "Builds every per-version jar and the universal jar."
    dependsOn(universalJar)
}
