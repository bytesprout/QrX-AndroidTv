plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":domain:domain-queue"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
}
