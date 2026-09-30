# network/api

The network contracts. Package `io.thernal.networkkit.network.api`.

## Contents

| Type | Is |
|---|---|
| `ApiClient` + `get`/`post`/`put`/`patch`/`delete` | calls that return the decoded body or throw `NetworkException` |
| `RequestBuilder` | `parameter`, `header`, `host` in a verb's block |
| `NetworkError`, `NetworkException`, `NetworkResult`, `networkCall { }`, `map`/`fold`/`getOrNull` | failures as values |
| `ErrorBody`, `FieldError` | a parsed error body |
| `ResponseUnwrapper`, `ErrorBodyParser` | for backends with envelopes or another error shape |
| `Interceptor` | a Ktor client plugin every client installs |
| `SessionManager`, `SessionState`, `Tokens`, `TokenStore`, `TokenRefresher`, `RefreshOutcome` | authentication |
| `AuthRoutes`, `ApiRoutePattern` | public and optional routes |
| `HttpCacheStore`, `CacheValidators`, `ConditionalResult`, `fetchConditional` | conditional requests |

## Installing

### Modules

| Module | Who depends on it |
|---|---|
| `:network:api` | every module with an API class, a repository or an interceptor |
| `:network:impl` | the module that builds the graph (or wires by hand) |
| `:network:wiring` | the module that declares the Metro graph |
| `:network:testing` | test source sets |

### Dependencies you declare

Nothing is re-exported (`api(...)` is not used): declare what a module's own code uses.

| Library | Where the module uses |
|---|---|
| `ktor-client-core` | `HttpResponse`, `HttpMethod`, `ContentType`, `ClientPlugin` (interceptors) |
| `kotlinx-coroutines-core` | the suspend calls, `SessionManager.state` |
| `kotlinx-serialization-json` + the serialization plugin | `@Serializable` DTOs; `JsonElement` in an unwrapper |

### With Metro

`NetworkWiring` provides `ApiClientFactory`. The app makes its clients — only it knows its base URLs:

```kotlin
@Provides @SingleIn(AppScope::class)
fun provideApiClient(factory: ApiClientFactory): ApiClient {
    return factory.create { Environment.BASE_URL }          // read on every request
}
```

Several hosts: one client each, under your own qualifiers.

`NetworkAuthWiring` provides `SessionManager` and the auth interceptor. The app binds a `TokenStore` and a
`TokenRefresher`, and contributes its routes — **the refresh route must be public**:

```kotlin
@Provides fun provideTokenStore(secure: SecureKeyValueStore): TokenStore { return SecureTokenStore(secure) }

@Provides fun provideTokenRefresher(client: ApiClient): TokenRefresher {
    return TokenRefresher { current ->
        val refresh = current.refreshToken ?: return@TokenRefresher null
        val dto: TokensDto = client.post("/auth/refresh", RefreshBody(refresh))
        Tokens(accessToken = dto.accessToken, refreshToken = dto.refreshToken)
    }
}

@Provides @IntoSet fun provideAuthRoutes(): AuthRoutes {
    return AuthRoutes(
        public = setOf(ApiRoutePattern("POST", "/auth/login"), ApiRoutePattern("POST", "/auth/refresh")),
        optional = setOf(ApiRoutePattern("GET", "/feed")),
    )
}
```

The refresher needs a client whose interceptors include the auth interceptor, which needs the session
manager, which needs the refresher: the manager takes it lazily, so the cycle compiles. An app without
sign-in excludes `NetworkAuthWiring` from its graph.

### Without a DI framework

```kotlin
val session = DefaultSessionManager(store = tokenStore, refresher = { current -> refresher.refresh(current) })
val factory = ApiClientFactory(interceptors = setOf(AuthInterceptor(session, setOf(routes))))
val client = factory.create { baseUrl }
```

## Calls

```kotlin
val post: PostDto = client.get("/posts/$id")
val page: List<PostDto> = client.get("/posts") { parameter("page", 2); header("X-Trace", trace) }
val created: PostDto = client.post("/posts", NewPost(title))
client.delete<Unit>("/posts/$id")                            // Unit reads no body
client.get<PostDto>("/avatar") { host("https://cdn.example.com") }   // one call, another host
client.uploadRaw(presignedUrl, bytes, contentType = ContentType.Image.JPEG)   // no interceptors
```

A `path` is appended to the base URL (`https://api.example.com/v1` + `/posts` → `…/v1/posts`).

## Failures

Every call throws `NetworkException(error)`:

| `NetworkError` | When |
|---|---|
| `NoConnection` | offline, DNS, refused or dropped connection |
| `Timeout` | connect, socket or request timeout |
| `Unauthorized(body)` | 401 that authentication did not recover, or a guest on a protected route |
| `Forbidden(body)` | 403 |
| `RateLimited(retryAfterSeconds)` | 429 |
| `Unavailable(status)` | 502, 503, 504 |
| `Http(status, body)` | any other non-success status; `body` is the parsed `ErrorBody`, field errors included |
| `Serialization(cause)` | a body that does not match its type |
| `Unexpected(cause)` | anything else an engine threw |

At the repository edge:

```kotlin
suspend fun posts(): NetworkResult<List<Post>> {
    return networkCall { api.posts(page = 1).map(PostDto::toDomain) }
}
```

Cancellation is never turned into a failure.

## Backends with an envelope or another error shape

```kotlin
@Provides @IntoSet fun provideUnwrapper(): ResponseUnwrapper {
    return ResponseUnwrapper { _, body ->
        val envelope = body.jsonObject
        if (envelope["success"]?.jsonPrimitive?.boolean == false) {
            throw NetworkException(NetworkError.Http(status = 200, body = ErrorBody(message = envelope["message"]?.jsonPrimitive?.content)))
        }
        envelope["data"] ?: JsonNull
    }
}
```

Contribute at most one unwrapper and one `ErrorBodyParser`; without one, the body is the response and error
bodies are read as `{code, error, message, fields: {name: {code, error}}}`.

## Sessions

```kotlin
session.state                         // StateFlow: Unknown → Guest | Authenticated; Expired when the server ends it
session.signIn(Tokens(access, refresh))
session.signOut()                     // → Guest
```

Observe `state` at the root: on `Expired`, show sign-in. Requests without a token go out as a guest; their
401 is just `Unauthorized`.

## Interceptors

```kotlin
class ClientHeaders(private val info: AppInfo) : Interceptor {
    override val order = 50
    override val plugin = createClientPlugin("ClientHeaders") {
        onRequest { request, _ -> request.headers.append("X-App-Version", info.version) }
    }
}
@Provides @IntoSet fun provideClientHeaders(info: AppInfo): Interceptor { return ClientHeaders(info) }
```

Lower `order` runs first; auth is `1`. A debug console's Ktor plugin is contributed the same way, in
non-production builds.

## Conditional cache

```kotlin
val config: RemoteConfigDto? = client.fetchConditional(store = httpCacheStore, path = "/config")
// null: 304 Not Modified — keep what you have
```

Validators live in an `HttpCacheStore`: `InMemoryHttpCacheStore` for the process, or the app's own over
storage to keep them across restarts.

## Testing

```kotlin
val client = mockApiClient { request ->
    when (request.url.encodedPath) {
        "/posts" -> respondJson("""[{"id": 1}]""")
        else -> respondJson("""{"message": "no"}""", HttpStatusCode.NotFound)
    }
}
```

`mockApiClient` is the real client — same decoding, errors and interceptors — over MockEngine, with no
timeouts (they would fire at once under `runTest`'s virtual time). `FakeTokenStore` and `FakeTokenRefresher`
drive a `DefaultSessionManager` in auth tests.
