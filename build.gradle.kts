plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "lib.fetchtele"

// 年份/月份/修订
version = "2025.2.3"

// 这个在gradle.properties里，记得跟进最新版本
val ktor_version: String by project
val jsoup_version: String by project

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-cio:$ktor_version")

    implementation("org.jsoup:jsoup:$jsoup_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
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