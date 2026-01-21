plugins {
    id("java-library")
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
    alias(libs.plugins.kotlinxKover)
}
dependencies {
    implementation(project(":core"))
    implementation(libs.junit)
}

tasks.test {
    useJUnit()
}
