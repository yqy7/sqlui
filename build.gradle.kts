import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    id("org.openjfx.javafxplugin") version "0.1.0"
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

// ===== jpackage: 创建自带 JVM 的独立 .app 应用 =====
tasks.register<Exec>("jpackageApp") {
    dependsOn(tasks.build)
    description = "使用 jpackage 创建独立应用（自带完整 JVM，约 300MB）"

    val jvmArgs = listOf(
        "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
    ).joinToString(" ")

    commandLine(
        "jpackage",
        "--name", "SQLui",
        "--type", "app-image",
        "--input", "build/libs",
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "io.github.yqy7.sqlui.AppKt",
        "--java-options", jvmArgs,
        "--dest", "build/jpackage",
        "--runtime-image", System.getProperty("java.home"),
        "--vendor", "yqy7",
        "--app-version", "$version",
        "--description", "SQLui - 数据库管理工具",
        "--mac-package-identifier", "io.github.yqy7.sqlui"
    )
}
