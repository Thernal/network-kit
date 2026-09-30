plugins {
    alias(libs.plugins.networkkit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            // Ktor's request and response types, coroutines' Flow and kotlinx.serialization's JSON
            // model are in these contracts' signatures, yet none is re-exported: `api(...)` is not
            // used in this repository, so a consumer declares each (README → Dependencies you declare).
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
