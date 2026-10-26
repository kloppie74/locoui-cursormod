pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "locoui-cursormod"

// One subproject per Minecraft generation; src/minecraft/<mc>/gradle.properties says which glue,
// which Fabric API and which Minecraft versions its build runs on. (versions/ is the history of
// released jars, one folder per mod version.)
val targets = listOf("1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2", "26.3")
for (mc in targets) {
    include(":mc-$mc")
    project(":mc-$mc").projectDir = file("src/minecraft/$mc")
}
