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
