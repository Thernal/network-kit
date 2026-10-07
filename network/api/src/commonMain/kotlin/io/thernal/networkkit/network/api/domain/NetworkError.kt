package io.thernal.networkkit.network.api.domain

/**
 * Why a call failed, in terms a repository can act on without catching a transport exception. The
 * kit knows nothing of an application's domain `Failure`; map these to it at the repository edge.
 */
sealed interface NetworkError {
    /** No route to the server: offline, DNS, a refused or dropped connection. */
    data object NoConnection : NetworkError

    data object Timeout : NetworkError

    /** 401 that authentication could not recover (or a guest calling a protected route). */
    data class Unauthorized(
        val body: ErrorBody? = null,
    ) : NetworkError

    /** 403: authenticated, but not allowed. */
    data class Forbidden(
        val body: ErrorBody? = null,
    ) : NetworkError

    /** 429, with the server's `Retry-After` in seconds when it sent one. */
    data class RateLimited(
        val retryAfterSeconds: Long? = null,
    ) : NetworkError

    /** 502, 503 or 504: the server, not the request, is the problem; retrying later may work. */
    data class Unavailable(
        val status: Int,
    ) : NetworkError

    /** Any other non-success status, with the error body the configured parser could read. */
    data class Http(
        val status: Int,
        val body: ErrorBody? = null,
    ) : NetworkError

    /** A body that did not match the type it was decoded into. */
    data class Serialization(
        val cause: Throwable,
    ) : NetworkError

    data class Unexpected(
        val cause: Throwable,
    ) : NetworkError
}
