import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

// Rechnerspezifische Einstellungen (nicht eingecheckt), z. B. platformLocalPath=/Applications/PhpStorm.app
val localProperties = Properties().apply {
    file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}
val platformLocalPath: String? = localProperties.getProperty("platformLocalPath")?.takeIf { file(it).exists() }

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Keine Brücken zu (veralteten) Default-Methoden der Plattform-Interfaces erzeugen,
        // sonst meldet verifyPlugin StatusBarWidget.getPresentation als genutzt.
        jvmDefault = org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY
    }
}

// ./gradlew runIde -Pscreenshots=<ordner> baut den Screenshot-Helfer mit ein (nie für Releases verwenden).
val screenshotDir = providers.gradleProperty("screenshots").orNull

sourceSets {
    main {
        if (screenshotDir != null) kotlin.srcDir("src/screenshot/kotlin")
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        if (platformLocalPath != null) {
            local(platformLocalPath)
        } else {
            create(IntelliJPlatformType.PhpStorm, providers.gradleProperty("platformVersion").get())
        }
        bundledPlugin("com.intellij.platform.testRunner")
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
        changeNotes = """
            <h3>1.0.1</h3>
            <ul>
              <li>Description updated: Grand Theft Auto VI is out.</li>
            </ul>
            <h3>1.0.0</h3>
            <ul>
              <li>First release: <i>MISSION PASSED</i> when tests pass or a run finishes cleanly, <i>WASTED</i> when they fail.</li>
              <li>Wanted level: up to five stars in the status bar for the warnings and errors in the current file.</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
            .orElse(providers.gradleProperty("jetbrainsMarketplaceToken"))
    }

    pluginVerification {
        ides {
            platformLocalPath?.let { local(it) }
            create(IntelliJPlatformType.PhpStorm, providers.gradleProperty("verifyOldestVersion").get())
            create(IntelliJPlatformType.PhpStorm, providers.gradleProperty("verifyNewestVersion").get())
        }
    }
}

// Apache 2.0 verlangt, dass LICENSE und NOTICE mit dem Plugin verteilt werden.
tasks.jar {
    from(files("LICENSE", "NOTICE")) { into("META-INF") }
}

// Für Screenshots wird das Demo-Projekt an einen neutralen Ort kopiert, damit kein lokaler Benutzerpfad im Bild erscheint.
val screenshotProjectDir = "/Users/Shared/mission-demo"

val prepareScreenshotProject = tasks.register<Sync>("prepareScreenshotProject") {
    from("marketplace/demo-project")
    into(screenshotProjectDir)
}

// Ohne -Pscreenshots bleibt aus dem Build-Cache sonst ein leeres screenshot/-Verzeichnis in den Jars.
tasks.withType<Zip>().configureEach {
    includeEmptyDirs = false
}

tasks.runIde {
    if (screenshotDir != null) {
        dependsOn(prepareScreenshotProject)
        args(screenshotProjectDir)
        jvmArgs("-Dmissionpassed.screenshots=${file(screenshotDir).absolutePath}")
        jvmArgs("-Dmissionpassed.screenshotSteps=${file("marketplace/screenshots.txt").absolutePath}")
    } else {
        providers.gradleProperty("openProject").orNull?.let { args(it) }
    }
}
