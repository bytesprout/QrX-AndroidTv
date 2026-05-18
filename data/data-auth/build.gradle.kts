plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-storage"))
    implementation(project(":domain:domain-auth"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
}
