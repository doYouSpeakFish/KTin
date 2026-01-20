plugins {
    id("java-library")
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinxKover)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}
dependencies {
    implementation(project(":core"))
    implementation(libs.junit)
}

tasks.test {
    useJUnit()
}
