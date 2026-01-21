plugins {
    id("java-library")
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
    alias(libs.plugins.kotlinxKover)
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
