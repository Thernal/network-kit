# network-kit

The network layer of a Compose Multiplatform app (android, iosArm64, iosSimulatorArm64), on Ktor: an
`ApiClient` whose calls return a decoded body or throw one `NetworkException`, interceptors, a
`SessionManager` that owns authentication (one token refresh for any number of failed requests, guests,
expiry), public and optional routes, envelope and error-body parsing for any backend, and a conditional
HTTP cache.

```kotlin
class PostApi(private val client: ApiClient) {
    suspend fun posts(page: Int): List<PostDto> {
        return client.get("/posts") { parameter("page", page) }
    }

    suspend fun create(body: NewPost): PostDto {
        return client.post("/posts", body)
    }
}

val result: NetworkResult<List<PostDto>> = networkCall { postApi.posts(page = 1) }
```

## Documentation

| Read | For |
|---|---|
| this file | what is here and how it is built |
| [`network/api/README.md`](network/api/README.md) | every contract, installation and use — task by task |
| [`network/README.md`](network/README.md) | why the contracts have their shape, and what changed from the apps it came from |
| [`skills/network-kit`](skills/network-kit/SKILL.md) | the same for an agent working in an app that uses the kit |
| [`docs/todos/`](docs/todos) | open questions |

## For AI agents

An application takes the kit by copy: `skillctl.sh kit install network-kit --package <its package>
--module <its module path> --alias <its plugin alias>` copies `network/api`, `impl`, `wiring` and
`testing` renamed, installs the `network-kit` skill, and records both in `kits.lock`. `kit.yml` lists
what the copied modules expect from the application.

## Layout

| Module | Holds | Depends on |
|---|---|---|
| `network/api` | `ApiClient` and its verbs, `ApiRequest`/`RequestBuilder`, `NetworkError`/`NetworkException`/`NetworkResult`/`networkCall`, `ResponseUnwrapper`, `ErrorBodyParser`, `Interceptor`, `SessionManager`/`TokenStore`/`TokenRefresher`/`AuthRoutes`/`ApiRoutePattern`, `HttpCacheStore`/`fetchConditional` | Ktor client core, coroutines, kotlinx.serialization |
| `network/impl` | `ApiClientFactory` (OkHttp on Android, Darwin on iOS), the client, `DefaultErrorBodyParser`, `DefaultSessionManager`, `AuthInterceptor`, `InMemoryHttpCacheStore` | api, Ktor |
| `network/wiring` | `NetworkWiring` (the factory, interceptor/unwrapper/parser sets), `NetworkAuthWiring` (session manager, auth interceptor, route set) | api, impl |
| `network/testing` | `mockApiClient` over Ktor's MockEngine, `respondJson`, `FakeTokenStore`, `FakeTokenRefresher` | api, impl, ktor-client-mock |

## Building

```sh
./gradlew build
```

Every target compiled, tests on the JVM host and the iOS simulator (the same MockEngine suites on both:
decoding, every error mapping, envelopes, the conditional cache, uploads, and authentication — including
five concurrent 401s sharing one refresh), and Detekt, which fails on any finding. `-PdetektAutoCorrect=true`
lets ktlint fix formatting first. The Gradle daemon runs on JDK 21 (Metro).
