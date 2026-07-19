import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.graalvm.buildtools.native") version "1.1.2"
    application
}

group = "io.github.yqy7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    version = "26.0.1"
    modules = listOf(
        "javafx.controls",
        "javafx.graphics",
        "javafx.base"
    )
}

dependencies {
    // Database drivers
    implementation("com.h2database:h2:2.4.240")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.1")

    // RichTextFX for SQL syntax highlighting
    implementation("org.fxmisc.richtext:richtextfx:0.11.7")

    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

application {
    mainClass.set("io.github.yqy7.sqlui.AppKt")
    applicationDefaultJvmArgs = listOf(
        "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
}

// ===== GraalVM Native Image 配置 =====
graalvmNative {
    binaries {
        named("main") {
            mainClass.set("io.github.yqy7.sqlui.AppKt")
            imageName.set("sqlui")
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
                "-H:+AddAllFileSystemProviders",
                // JavaFX 模块访问
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                // 允许 incomplete classpath（可选依赖）
                "--allow-incomplete-classpath",
                // 包含所有字符集
                "-H:+AddAllCharsets",
            )
            // 包含资源文件
            resources.autodetect()
        }
    }
}
