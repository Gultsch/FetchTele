plugins {
    kotlin("jvm") version "2.0.21"
}

group = "lib.fetchtele"

// 年份/月份/修订
version = "2025.1.1"

val ktor_version: String by project
val jsoup_version: String by project

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jsoup:jsoup:$jsoup_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}