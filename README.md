# network-kit

The network layer of a Compose Multiplatform app (android, iosArm64, iosSimulatorArm64), on Ktor: an
`ApiClient` whose calls return a decoded body or throw one `NetworkException`, interceptors, a
`SessionManager` that owns authentication (one token refresh for any number of failed requests, guests,
expiry), public and optional routes, envelope and error-body parsing for any backend, automatic retries of
idempotent requests, connectivity observation, and a conditional HTTP cache.

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

## Installing

An application takes the kit **by copy**, not as a dependency: the code is copied into the app, renamed to the app's own package, and belongs to the app from then on. Nothing is published to a Maven repository.

### With skill-manager

If you have access to the author's knowledge repository (`github.com/Thernal/knowledge`), its **skill-manager** skill does all of it — copy, rename, the skill, and later updates:

```sh
skillctl.sh kit install network-kit --package com.example.app --module :core:network --alias app
```

It copies the `code` parts of [`kit.yml`](kit.yml) renamed, installs the `network-kit` skill and records the copy in `kits.lock`. `kit status` then shows what changed upstream and what the app edited; `kit update` merges the kit's changes three ways, keeping the app's edits. The install prints what the app must provide (`requires`).

### Without it

The same by hand, from a clone of this repository.

1. **Copy** the paths listed under `code` in [`kit.yml`](kit.yml) into the app, under the module path the app gives them: `network/…` → `core/network/…`. Note the commit you copied (`git rev-parse HEAD`) — updates start from it.
2. **Rename** in everything copied:

   | In the kit | Becomes | Where |
   |---|---|---|
   | `io.thernal.networkkit` | the app's package, e.g. `com.example.app` | sources, build files; and the directories `io/thernal/networkkit` |
   | `:network:` and `":network"`, `projects.network.` | the module path, e.g. `:core:network:`, `projects.core.network.` | build files |
   | `libs.plugins.networkkit.` | the app's catalog alias, e.g. `libs.plugins.app.` | build files |

   ```sh
   # in the app, after copying — perl, so it runs the same on macOS and Linux
   grep -rlI -e io.thernal.networkkit -e io/thernal/networkkit -e :network -e plugins.networkkit. core/network \
     | xargs perl -pi -e 's/\Qio.thernal.networkkit\E/com.example.app/g; s{\Qio/thernal/networkkit\E}{com/example/app}g; s/\Q:network:\E/:core:network:/g; s/"\Q:network\E"/":core:network"/g; s/projects\.\Qnetwork\E\./projects.core.network./g; s/libs\.plugins\.\Qnetworkkit\E\./libs.plugins.app./g'
   find core/network -depth -type d -path '*/io/thernal/networkkit' | while read -r d; do
     mkdir -p "${d%/io/thernal/networkkit}/com/example" && mv "$d" "${d%/io/thernal/networkkit}/com/example/app"
   done
   find core/network -depth -type d -empty -delete
   ```

3. **Provide** what the copy expects — the `requires` list in [`kit.yml`](kit.yml): convention plugins (build-kit's, or the ones in this repository's `build-logic/convention`), catalog entries, settings — and, where listed, platform setup.
4. **The skill** (optional): copy [`skills/network-kit`](skills/network-kit) into the app's skills directory (`.claude/skills/` for Claude Code), with the same renames, so an agent working in the app knows the kit.
5. **Updates** are yours to carry: `git diff <the commit you copied> <a newer one> -- <the code paths>` in the kit shows what changed; apply what you want, renamed the same way.

## Layout

| Module | Holds | Depends on |
|---|---|---|
| `network/api` | `ApiClient` and its verbs, `ApiRequest`/`RequestBuilder`, `NetworkError`/`NetworkException`/`NetworkResult`/`networkCall`, `ResponseUnwrapper`, `ErrorBodyParser`, `Interceptor`, `SessionManager`/`TokenStore`/`TokenRefresher`/`AuthRoutes`/`ApiRoutePattern`, `ConnectivityMonitor`, `HttpCacheStore`/`fetchConditional` | Ktor client core, coroutines, kotlinx.serialization |
| `network/impl` | `ApiClientFactory` (OkHttp on Android, Darwin on iOS), the client, `ErrorBodyParserImpl`, `SessionManagerImpl`, `AuthInterceptor`, `RetryInterceptor`, connectivity (`ConnectivityManager`, `NWPathMonitor`), `InMemoryHttpCacheStore` | api, Ktor |
| `network/wiring` | `NetworkProvidersModule` (the factory, interceptor/unwrapper/parser sets), `NetworkAuthProvidersModule` (session manager, auth interceptor, route set), `NetworkResilienceProvidersModule` + `NetworkConnectivity{Android,Ios}Wiring` (retries, connectivity) | api, impl |
| `network/testing` | `mockApiClient` over Ktor's MockEngine, `respondJson`, `FakeTokenStore`, `FakeTokenRefresher`, `FakeConnectivityMonitor` | api, impl, ktor-client-mock |

## Building

```sh
./gradlew build
```

Every target compiled, tests on the JVM host and the iOS simulator (the same MockEngine suites on both:
decoding, every error mapping, envelopes, the conditional cache, uploads, retries — backoff, `Retry-After`,
nothing retried while offline or for POST — and authentication, including five concurrent 401s sharing one
refresh; `NWPathMonitor` runs for real on the simulator), and Detekt, which fails on any finding. `-PdetektAutoCorrect=true`
lets ktlint fix formatting first. The Gradle daemon runs on JDK 21 (Metro).
