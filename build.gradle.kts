
plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version("1.17.20") apply(false)
}

val MINECRAFT_VERSION by extra { "26.3" }
val NEOFORGE_VERSION by extra { "26.3.0.7-beta" }
val FABRIC_LOADER_VERSION by extra { "0.19.3" }
val FABRIC_API_VERSION by extra { "0.160.5+26.3" }

val SODIUM_DEPENDENCY_FABRIC by extra { "net.caffeinemc:sodium-fabric:0.9.2+mc26.3" }
val SODIUM_DEPENDENCY_NEO by extra { "net.caffeinemc:sodium-neoforge-mod:0.9.2+mc26.3" }

// This value can be set to null to disable Parchment.
// TODO: Re-add Parchment
val PARCHMENT_VERSION by extra { null }

// WynnIris versioning: IRIS_BASE_VERSION is the upstream Iris version we forked from.
// It's used as the mod version so Fabric/Sodium compatibility checks pass.
// WYNNIRIS_VERSION is our own release counter, used in the jar filename only.
val WYNNIRIS_VERSION by extra { "1.2.2" }
val IRIS_BASE_VERSION by extra { "1.11.6" }
val MOD_VERSION by extra { IRIS_BASE_VERSION }

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
}

subprojects {
    apply(plugin = "maven-publish")

    java.toolchain.languageVersion = JavaLanguageVersion.of(25)


    // Mod metadata version — must look like a real Iris version for Sodium compatibility
    fun modVersionString(): String = "${MOD_VERSION}+mc${MINECRAFT_VERSION}"

    // User-facing jar filename version
    fun archiveVersionString(): String {
        val isReleaseBuild = project.hasProperty("build.release")
        val suffix = if (isReleaseBuild) "" else "-dev"
        return "${WYNNIRIS_VERSION}${suffix}+mc${MINECRAFT_VERSION}"
    }

    tasks.processResources {
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf("version" to modVersionString()))
        }
    }

    version = modVersionString()
    group = "net.irisshaders"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    // Disables Gradle's custom module metadata from being published to maven. The
    // metadata includes mapped dependencies which are not reasonably consumable by
    // other mod developers.
    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}
