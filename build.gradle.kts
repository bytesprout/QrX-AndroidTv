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
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
