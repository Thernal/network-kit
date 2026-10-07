package io.thernal.networkkit.network.api.data.auth

/**
 * How authentication treats routes, contributed by whoever owns them (a feature's wiring):
 *
 * - [public] — never carries a token: sign-in, registration, the token refresh itself. **The refresh
 *   route must be here**, or a rejected refresh would wait on itself.
 * - [optional] — carries a token when there is one; when authentication fails for good, it is retried
 *   as a guest instead of ending the session: a feed that signed-in users see personalised.
 *
 * Every other route is protected. ArenaGo called these `UnprotectedApiRoutes` and `IgnoredApiRoutes`.
 */
data class AuthRoutes(
    val public: Set<ApiRoutePattern> = emptySet(),
    val optional: Set<ApiRoutePattern> = emptySet(),
)
