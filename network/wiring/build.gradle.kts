plugins {
    alias(libs.plugins.networkkit.kmp.library)
    alias(libs.plugins.networkkit.injection)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // The contracts are not re-exported: an app that injects them depends on `api` itself.
                implementation(projects.network.api)
                implementation(projects.network.impl)
            }
        }
    }
}
