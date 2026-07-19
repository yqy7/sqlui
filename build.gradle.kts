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
    implementation("com.h2database:h2:2.4.240")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.1")
    implementation("org.fxmisc.richtext:richtextfx:0.11.7")
    testImplementation(kotlin("test"))
}

// JVM 22 兼容 GraalVM 22.0.1 的 native-image
java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_22)
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

// ===== GraalVM Native Image (AOT 编译) =====
// 使用 GraalVM 22.0.1 的 native-image（JDK 25 Liberica NIK 的 JavaFXFeature 有 bug）
val graalvmHome: String = System.getenv("GRAALVM_HOME")
    ?: project.findProperty("graalvmHome") as String?
    ?: "/Users/yqy/.sdkman/candidates/java/22.0.1-graal"

tasks.register<Exec>("nativeCompile") {
    dependsOn(tasks.installDist)
    description = "使用 GraalVM Native Image 编译为原生可执行文件"

    val libDir = layout.buildDirectory.dir("install/sqlui/lib").get().asFile.absolutePath
    val classpath = fileTree(libDir).files.joinToString(":") { it.absolutePath }

    commandLine(
        "$graalvmHome/bin/native-image",
        "--no-fallback",
        "-cp", classpath,
        "-H:+ReportExceptionStackTraces",
        "-H:Name=sqlui",
        "io.github.yqy7.sqlui.AppKt"
    )
}

// ===== jpackage 备用方案 =====
tasks.register<Exec>("jpackageApp") {
    dependsOn(tasks.build)
    description = "使用 jpackage 创建独立 .app 应用（自带 JVM，备用）"

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
