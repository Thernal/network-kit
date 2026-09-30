# network — design

Why the contracts have their shape. What they are and how to use them: [`api/README.md`](api/README.md).

The kit is ArenaGo's `core/network` and the auth interceptor from its authentication feature, ported to
Compose Multiplatform, with Act2Act's envelope handling, per-host clients and conditional cache — each
choice decided explicitly (epic decisions D34–D38).

## One exception, one result

Every failure a call can meet — no connection, a timeout, a status, a body that does not decode, anything
an engine throws — becomes one `NetworkException` carrying a `NetworkError`. Features never catch a Ktor or
platform exception, and the list of errors is the list of things a repository can act on. `networkCall { }`
turns it into a `NetworkResult` at the edge; verbs themselves throw, because a repository usually composes
several calls into one result, and result-returning verbs make that noisy.

The kit does not know an application's domain `Failure` (arch-kit's, or the app's own). `NetworkError.Http`
carries the status and the parsed error body — field errors included — so that mapping is one function in
the app, and neither kit depends on the other.

## The body is the response; envelopes are the app's

ArenaGo's backend returns bodies as they are; Act2Act's wraps each in `{success, message, data}`. A kit
cannot assume either, so the body is the response unless the app registers a `ResponseUnwrapper`, which
sees the JSON and returns the part to decode — or throws for an envelope that reports failure under a 200.
Error bodies go through an `ErrorBodyParser`; the default reads ArenaGo's `{code, error, message, fields}`.
Decoding goes through a JSON element for exactly this: an unwrapper can open any envelope without the kit
knowing its type.

## Authentication, split in two

ArenaGo's `AuthInterceptor` did everything — tokens, refresh, retries, ending the session — inside a Ktor
plugin, and the app learned about an ended session through its repository. Here the two halves are apart:

- **`SessionManager`** owns the session: its `StateFlow<SessionState>` (`Unknown`, `Guest`,
  `Authenticated`, `Expired`), the tokens (read from the app's `TokenStore` once, kept in memory — a secure
  store decrypts on every read), sign-in, sign-out, and refreshing. A refresh is **single-flight**: every
  request that failed with the same stale token waits for one refresh; a request whose token was already
  replaced gets `Refreshed` without another. The refresh runs in the manager's own scope, so one waiting
  caller being cancelled does not cancel it for the others.
- **`AuthInterceptor`** is only HTTP: attach the token, notice a 401, ask the manager, retry (at most three
  times), fall back to a guest request on optional routes.

The app implements two small things — `TokenStore` (over its secure storage) and `TokenRefresher` (its
refresh endpoint) — and watches `state`: `Expired` is where it sends the user to sign in. A rejected refresh
expires the session — null from the refresher, or a status the refresh endpoint answered with; a refresh that
could not be tried (offline, timeout, the server unavailable) keeps it; fresh tokens the server keeps rejecting
expire it after the retries.

No token is a guest, not an error: requests go out without `Authorization`, and a 401 refreshes and ends
nothing — the caller gets `Unauthorized`.

## Routes by name, not in code

ArenaGo hard-coded its unprotected and ignored routes in the network module. Here each feature contributes
`AuthRoutes` — `public` (never a token; the refresh endpoint must be one, or a rejected refresh would wait on
itself) and `optional` (a token if there is one; a guest retry if auth fails for good) — matched by method
and path segments with `*` and `{placeholder}`.

## One client per host

Act2Act talks to several hosts; ArenaGo to one with a per-request override. `ApiClientFactory.create(baseUrl)`
makes a client per host over one shared Ktor client, and reads the base URL on every request, so a debug
console that switches environments at run time works without rebuilding anything. Presigned uploads go
through a second Ktor client with no interceptors: no token and no app headers reach a storage provider.

## Retries: only what is safe, never against no network

Neither source app retried. The kit does, within limits that make it safe to have on by default: only
idempotent methods (a retried POST can create twice), only failures a second try can fix (a dropped
connection, a timeout, 502/503/504, 429), at most two retries with exponential backoff and jitter — or the
server's `Retry-After`, unless that is longer than the caller should wait. While the device is offline it
does not retry at all: backing off against no network only delays the `NoConnection` the screen has to show.
It sits inside authentication (order 10 after 1), so an authenticated attempt is what is retried.

`ConnectivityMonitor` is the platform's own view — `ConnectivityManager`'s validated internet capability on
Android, `NWPathMonitor` on iOS — as a `StateFlow`. It says a route exists, not that the server answers.
Retries and connectivity have their own wiring containers, so an app that wants neither excludes them.

## What is not here

The debug console, logging and app headers are interceptors the app contributes — the kit depends on no
other kit. Ktor's body cache is not used: the apps cache validators and keep bodies themselves, which is
what `fetchConditional` supports.
