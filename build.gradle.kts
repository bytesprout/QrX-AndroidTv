plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(17)
        }

        dependencies {
            add("testImplementation", "org.junit.jupiter:junit-jupiter:5.13.0")
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
            add("testImplementation", "io.mockk:mockk:1.14.0")
            add("testImplementation", "app.cash.turbine:turbine:1.2.0")
            add("testImplementation", "com.google.truth:truth:1.4.4")
            add("testImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
