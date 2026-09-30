# Setup

`skillctl.sh kit install network-kit --package <app package> --module <module path> --alias <plugin alias>`,
then include the four modules and provide what `kit.yml`'s `requires` lists (conventions
`<alias>.kmp.library` / `<alias>.injection`, the serialization plugin, Ktor artifacts incl. OkHttp,
Darwin and mock, kotlinx-serialization-json, coroutines, Metro).

Without skill-manager, the kit's `README.md` → Installing → *Without it* does the same by hand (copy, rename, provide).

## Dependencies

```kotlin
commonMain.dependencies {
    implementation(projects.network.api)           // API classes, repositories, interceptors
    implementation(libs.ktor.client.core)          // only where Ktor types appear in the module's own code
    implementation(libs.kotlinx.serialization.json)
}
// the graph module
commonMain.dependencies { implementation(projects.network.impl); implementation(projects.network.wiring) }
commonTest.dependencies { implementation(projects.network.testing) }
```

## Bindings (Metro)

```kotlin
@Provides @SingleIn(AppScope::class)
fun provideApiClient(factory: ApiClientFactory): ApiClient { return factory.create { Environment.BASE_URL } }

@Provides fun provideTokenStore(secure: SecureKeyValueStore): TokenStore { return SecureTokenStore(secure) }

@Provides fun provideTokenRefresher(client: ApiClient): TokenRefresher {
    return TokenRefresher { current ->
        val refresh = current.refreshToken ?: return@TokenRefresher null   // null = session over
        val dto: TokensDto = client.post("/auth/refresh", RefreshBody(refresh))
        Tokens(accessToken = dto.accessToken, refreshToken = dto.refreshToken)
    }
}

@Provides @IntoSet fun provideAuthRoutes(): AuthRoutes {
    return AuthRoutes(public = setOf(ApiRoutePattern("POST", "/auth/login"), ApiRoutePattern("POST", "/auth/refresh")))
}
```

`TokenRefresher` calls the endpoint and lets its `NetworkException` through: a rejection the server
answered (401, 403, another 4xx) ends the session; `NoConnection`, `Timeout`, `Unavailable`, `RateLimited`
keep it. Return null only when there is no refresh token. No sign-in in the app → exclude `NetworkAuthWiring` from the graph.

Several hosts: one `@Provides` per host with a qualifier, each `factory.create { thatBaseUrl }`.
