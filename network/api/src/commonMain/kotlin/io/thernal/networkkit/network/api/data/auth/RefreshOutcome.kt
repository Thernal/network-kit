package io.thernal.networkkit.network.api.data.auth

enum class RefreshOutcome {
    /** New tokens are in place — or another request already refreshed past the stale token. */
    Refreshed,

    /** The server rejected the refresh; the session is now [SessionState.Expired]. */
    Rejected,

    /** The refresh could not be tried (offline, timeout); the session is kept. */
    Failed,
}
