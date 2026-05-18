plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-storage"))
    implementation(project(":domain:domain-device"))
    implementation(project(":domain:domain-auth"))
    implementation(project(":data:data-device"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
