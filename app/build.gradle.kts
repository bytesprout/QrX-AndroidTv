plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":core:core-player"))
    implementation(project(":core:core-realtime"))
    implementation(project(":core:core-storage"))
    implementation(project(":core:core-database"))
    implementation(project(":domain:domain-queue"))
    implementation(project(":feature:feature-signage"))
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass.set("com.queuerx.tv.QueueRxTvBootstrapKt")
}
