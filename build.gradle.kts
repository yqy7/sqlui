import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    application
    id("org.graalvm.buildtools.native") version "0.11.5"
}

group = "io.github.yqy7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // 数据库驱动
    implementation("com.h2database:h2:2.4.240")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // Swing 现代化主题
    implementation("com.formdev:flatlaf:3.5.4")

    // SQL 编辑器（语法高亮、代码折叠）
    implementation("com.fifesoft:rsyntaxtextarea:3.5.2")

    // 测试
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-framework-datatest:5.9.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

application {
    mainClass.set("io.github.yqy7.sqlui.AppKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("sqlui")
            mainClass.set("io.github.yqy7.sqlui.AppKt")

            // AWT/Swing 运行时初始化
            buildArgs.add("--initialize-at-run-time=sun.awt,sun.java2d,sun.font,sun.lwawt,sun.lwawt.macosx")
            buildArgs.add("--initialize-at-run-time=java.awt.Toolkit,java.awt.GraphicsEnvironment")
            buildArgs.add("--initialize-at-run-time=com.apple.laf.AquaLookAndFeel")
            buildArgs.add("--initialize-at-run-time=com.formdev.flatlaf.FlatLightLaf")

            // 错误堆栈
            buildArgs.add("-H:+ReportExceptionStackTraces")

            // JNI / 反射配置（自动从 META-INF/native-image 加载）
        }
    }
}

// Native Image 编译后将 JDK AWT 原生库复制到输出目录，并生成启动脚本
tasks.named("nativeCompile") {
    doLast {
        val jdkLib = "${System.getenv("JAVA_HOME")}/lib"
        val outputDir = file("${layout.buildDirectory.get().asFile}/native/nativeCompile")
        val exeFile = file("$outputDir/sqlui")

        // 完整的 AWT/Swing 依赖链
        val libs = listOf(
            "libawt.dylib",
            "libawt_lwawt.dylib",
            "libfontmanager.dylib",
            "libfreetype.dylib",
            "libjava.dylib",
            "libjawt.dylib",
            "libjavajpeg.dylib",
            "liblcms.dylib",
            "libmlib_image.dylib",
            "libosxapp.dylib",
            "libosxui.dylib",
            "libsplashscreen.dylib"
        )

        // 1. 复制 JDK 原生库到输出目录
        for (lib in libs) {
            val src = file("$jdkLib/$lib")
            if (src.exists()) {
                copy {
                    from(src)
                    into(outputDir)
                }
                println("  ✓ Copied $lib")
            }
        }

        // libjvm.dylib 在 lib/server/ 子目录
        val jvmLib = file("$jdkLib/server/libjvm.dylib")
        if (jvmLib.exists()) {
            copy {
                from(jvmLib)
                into(outputDir)
            }
            println("  ✓ Copied libjvm.dylib (from server/)")
        }

        // 2. 生成启动脚本（设置 DYLD_LIBRARY_PATH 使 dyld 能找到 @rpath 依赖）
        val launchScript = file("$outputDir/sqlui.sh")
        launchScript.writeText("" +
            "#!/bin/bash\n" +
            "DIR=\"\$(cd \"\$(dirname \"\$0\")\" && pwd)\"\n" +
            "export DYLD_LIBRARY_PATH=\"\$DIR:\$DYLD_LIBRARY_PATH\"\n" +
            "exec \"\$DIR/sqlui\"\n"
        )
        launchScript.setExecutable(true)
        println("  ✓ Created launch script: ${launchScript.absolutePath}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
