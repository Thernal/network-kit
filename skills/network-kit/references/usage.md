# Usage

## Calls

```kotlin
client.get<PostDto>("/posts/$id")
client.get<List<PostDto>>("/posts") { parameter("page", page) }
client.post<NewPost, PostDto>("/posts", body)
client.delete<Unit>("/posts/$id")
client.get<Avatar>("/a") { host("https://cdn.example.com") }
client.uploadRaw(url, bytes, contentType = ContentType.Image.JPEG)   // presigned: no auth, no app headers
```

## Failures

`NetworkException(error)`: `NoConnection`, `Timeout`, `Unauthorized(body)`, `Forbidden(body)`,
`RateLimited(retryAfterSeconds)`, `Unavailable(status)`, `Http(status, body)`, `Serialization`, `Unexpected`.

```kotlin
suspend fun load(): NetworkResult<List<Post>> {
    return networkCall { api.posts().map { it.toDomain() } }
}
when (val error = (result as NetworkResult.Failure).error) {
    is NetworkError.Http -> error.body?.fields          // form field errors
    NetworkError.NoConnection -> …
    else -> …
}
```

## Sessions

```kotlin
session.signIn(Tokens(access, refresh))      // after a successful login call
session.signOut()                            // → Guest
session.state.collect { if (it == SessionState.Expired) navigateToSignIn() }
```

Guests: no token, no `Authorization`; a protected route answers `Unauthorized`. Optional routes
(`AuthRoutes.optional`) fall back to a guest request when auth fails for good.

## Interceptors

```kotlin
class ClientHeaders : Interceptor {
    override val order = 50
    override val plugin = createClientPlugin("ClientHeaders") { onRequest { request, _ -> request.headers.append("X-Platform", "ios") } }
}
@Provides @IntoSet fun provideClientHeaders(): Interceptor { return ClientHeaders() }
```

## Envelope backends

Contribute one `ResponseUnwrapper` (`JsonElement` → the part to decode, or throw `NetworkException`) and, if
errors differ, one `ErrorBodyParser`.

## Retries and connectivity

Idempotent requests are retried automatically (`NetworkResilienceWiring`): drops, timeouts, 502/503/504, 429
with `Retry-After`; at most twice; never POST/PATCH; never while offline. Do not add another retry loop on top.

```kotlin
connectivity.status.collect { banner(it == Connectivity.Offline) }   // ConnectivityMonitor
```

Android: `ACCESS_NETWORK_STATE` in the manifest.

## Conditional cache

`client.fetchConditional<T>(store, path)` → the body, or null on 304. `InMemoryHttpCacheStore`, or your own
`HttpCacheStore` over storage.

## Testing

```kotlin
val client = mockApiClient { request -> respondJson("""{"id": 1}""") }
val session = DefaultSessionManager(FakeTokenStore(Tokens("a")), FakeTokenRefresher { Tokens("b") })
val authed = mockApiClient(interceptors = setOf(AuthInterceptor(session, setOf(routes)))) { … }
```

## Troubleshooting

| Symptom | Cause |
|---|---|
| a request hangs after a 401 | the refresh endpoint is not in `AuthRoutes.public` — the refresh waits on itself |
| users logged out on a flaky network | the refresher catches failures and returns null — let its `NetworkException` through |
| 401 right after sign-in | `session.signIn` was not called, or the store writes but the manager was bypassed |
| `Serialization` errors on success | the backend wraps bodies — add a `ResponseUnwrapper` |
| a request is sent twice | a retry of an idempotent request — make the endpoint idempotent, or use POST |
| every test call times out | a test client with timeouts under `runTest` — use `mockApiClient` or `NetworkConfig(timeouts = null)` |
| `Metro: cycle` involving `TokenRefresher` | the refresher was injected eagerly somewhere; the session manager takes it lazily — do the same |
