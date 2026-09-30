plugins {
    alias(libs.plugins.networkkit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.network.api)
                implementation(projects.network.impl)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}
