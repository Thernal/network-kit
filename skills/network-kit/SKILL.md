---
name: network-kit
description: Builds, wires, reviews and debugs HTTP calls in Compose Multiplatform apps that use network-kit, the Ktor kit in packages io.thernal.networkkit.network.* (ApiClient, get/post/put/patch/delete, networkCall, NetworkResult, NetworkError, NetworkException, SessionManager, SessionState, TokenStore, TokenRefresher, AuthRoutes, ApiRoutePattern, ResponseUnwrapper, ErrorBodyParser, Interceptor, ApiClientFactory, fetchConditional, mockApiClient). Use it for any network work in such a project, even when network-kit is not named - API classes and repositories, error handling, sign-in and sign-out, token refresh, guest access, session expiry, public or optional endpoints, several hosts or a run-time environment switch, response envelopes, custom headers and logging, presigned uploads, ETag caching, and tests with MockEngine; and for problems such as requests hanging on refresh, users logged out unexpectedly, or 401s after sign-in. Not for projects calling Ktor or another HTTP client directly without the kit.
---

# network-kit

network-kit is the Ktor network layer of a Compose Multiplatform app: an `ApiClient` whose calls return a
decoded body or throw one `NetworkException`, a `SessionManager` that owns authentication, interceptors,
route policies, envelope and error-body parsing, and a conditional cache. Guide:
https://github.com/Thernal/network-kit — `network/api/README.md`.

## 1. Orient first

```sh
grep -rn --include=*.kt -e "ApiClientFactory" -e "factory.create" .            # clients and their base URLs
grep -rn --include=*.kt -e ": TokenStore" -e "TokenRefresher {" -e "AuthRoutes(" .   # auth bindings and routes
grep -rn --include=*.kt -e ": Interceptor" -e "ResponseUnwrapper" -e "ErrorBodyParser" .
grep -rn --include=*.kt -e "session.state" -e "SessionState.Expired" .          # who reacts to expiry
grep -rn --include=*.kt -e "networkCall {" .                                     # repository edges
```

Nothing installed → [references/setup.md](references/setup.md). **Taken as a kit?** A `kits.lock` naming
`network-kit` means the modules were copied renamed with skill-manager — this skill too. `skillctl.sh kit
status network-kit` says what moved upstream; offer `kit update network-kit` rather than hand edits.

## 2. The model

- API classes call the verbs: `client.get<T>(path) { parameter(…) }`, `client.post<B, T>(path, body)`.
  Every failure is `NetworkException(NetworkError)`; repositories wrap calls in `networkCall { }` →
  `NetworkResult`, then map `NetworkError` to the app's own failure type.
- `SessionManager` owns tokens and `state`; the auth interceptor only attaches, retries and falls back.
  The app implements `TokenStore` and `TokenRefresher`, contributes `AuthRoutes`, and reacts to
  `SessionState.Expired`.
- One `ApiClient` per host from `ApiClientFactory.create { baseUrl }`.

## 3. Tasks

| Task | Read |
|---|---|
| install, bind clients, TokenStore, TokenRefresher, routes; several hosts | [setup.md](references/setup.md) |
| calls, failures, sessions, interceptors, envelopes, cache, uploads, tests, troubleshooting | [usage.md](references/usage.md) |

## 4. Rules

- Never catch a Ktor or I/O exception in feature code; catch `NetworkException` or use `networkCall`.
- Never refresh tokens or attach `Authorization` by hand — that is the session manager's and the interceptor's.
- The refresh endpoint (and sign-in, registration) must be in `AuthRoutes.public`.
- Never swallow `CancellationException`.
- Store tokens only through `TokenStore`, and back it with secure storage.
- Test API classes and repositories with `mockApiClient`, not a real server.

## 5. Verify

```sh
./gradlew build
```
