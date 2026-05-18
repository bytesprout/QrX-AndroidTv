pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "QrX-AndroidTv"

include(":app")
include(":build-logic")

include(":core:core-common")
include(":core:core-ui")
include(":core:core-designsystem")
include(":core:core-navigation")
include(":core:core-network")
include(":core:core-player")
include(":core:core-realtime")
include(":core:core-storage")
include(":core:core-database")
include(":core:core-analytics")
include(":core:core-telemetry")
include(":core:core-crash")
include(":core:core-security")
include(":core:core-kiosk")
include(":core:core-testing")
include(":core:core-utils")

include(":domain:domain-auth")
include(":domain:domain-queue")
include(":domain:domain-media")
include(":domain:domain-display")
include(":domain:domain-device")
include(":domain:domain-health")
include(":domain:domain-settings")

include(":data:data-auth")
include(":data:data-device")
include(":data:data-media")
include(":data:data-queue")
include(":data:data-realtime")
include(":data:data-cache")
include(":data:data-settings")
include(":data:data-telemetry")

include(":feature:feature-splash")
include(":feature:feature-provisioning")
include(":feature:feature-registration")
include(":feature:feature-home")
include(":feature:feature-signage")
include(":feature:feature-media")
include(":feature:feature-queue")
include(":feature:feature-doctor")
include(":feature:feature-pharmacy")
include(":feature:feature-emergency")
include(":feature:feature-ticker")
include(":feature:feature-maintenance")
include(":feature:feature-settings")
include(":feature:feature-health")
include(":feature:feature-diagnostics")
include(":feature:feature-kiosk")
