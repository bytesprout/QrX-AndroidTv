plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":domain:domain-auth"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}
