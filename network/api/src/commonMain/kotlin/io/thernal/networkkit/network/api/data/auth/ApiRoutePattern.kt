package io.thernal.networkkit.network.api.data.auth

/**
 * A route by method and path, segment by segment: `*` and `{placeholder}` match any one segment, a
 * null [method] any method. `ApiRoutePattern("POST", "/api/v1/users/{id}/avatar")`.
 */
data class ApiRoutePattern(
    val method: String?,
    val path: String,
) {
    private val segments = segmentsOf(path)

    fun matches(
        method: String,
        path: String,
    ): Boolean {
        if (this.method != null && !this.method.equals(other = method, ignoreCase = true)) {
            return false
        }
        val actual = segmentsOf(path)
        return segments.size == actual.size &&
            segments.zip(
                actual,
            ).all { (expected, value) -> expected == "*" || expected.isPlaceholder() || expected == value }
    }

    private fun String.isPlaceholder(): Boolean {
        return startsWith('{') && endsWith('}')
    }

    private companion object {
        fun segmentsOf(path: String): List<String> {
            return path.substringBefore('?').split('/').filter(String::isNotBlank)
        }
    }
}

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
