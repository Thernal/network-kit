package io.thernal.networkkit.network.api.data.auth

sealed interface SessionState {
    /** Not read from the store yet. */
    data object Unknown : SessionState

    /** No tokens: requests go out without `Authorization`. */
    data object Guest : SessionState

    data object Authenticated : SessionState

    /** The server rejected the session; the tokens are gone. Show sign-in, then [SessionManager.signIn]. */
    data object Expired : SessionState
}
