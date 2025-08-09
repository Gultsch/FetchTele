plugins {
    kotlin("jvm") version "2.1.21" // 检查更新：https://kotlinlang.org/
    id("com.gradleup.shadow") version "9.0.0-beta15" // 检查更新：https://plugins.gradle.org/plugin/com.gradleup.shadow
}

group = "lib.fetchtele"

// 年份/月份/修订
version = "2025.6.2"

// 记得跟进最新版本
val ktorVersion = "3.1.3" // 检查更新：https://ktor.io/
val jsoupVersion = "1.20.1" // 检查更新：https://jsoup.org/

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("deps-inlined")
    }

    jar {
        archiveClassifier.set("deps-not-inlined")
    }

    register("packLibrary") {
        group = "build"
        description = "打包内联依赖以及未内联的库"

        dependsOn("shadowJar", "jar")

        doLast {
            println("内联依赖以及未内联的库打包完成，参见build/libs/")
        }
    }
}

kotlin {
    jvmToolchain(21)
}