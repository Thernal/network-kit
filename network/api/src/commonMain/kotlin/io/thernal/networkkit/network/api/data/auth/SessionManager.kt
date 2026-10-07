package io.thernal.networkkit.network.api.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the session: its [state], the tokens through [TokenStore], and renewing them through
 * [TokenRefresher]. The auth interceptor asks it for the token and to refresh; nothing else in the
 * network layer knows about tokens. Screens observe [state] — an [SessionState.Expired] is where the
 * app sends the user to sign in.
 */
interface SessionManager {
    val state: StateFlow<SessionState>

    /** The current access token, or null for a guest. */
    suspend fun accessToken(): String?

    suspend fun signIn(tokens: Tokens)

    /** The user signed out: tokens cleared, state [SessionState.Guest]. */
    suspend fun signOut()

    /**
     * Renews the tokens once for every caller that saw [staleAccessToken] fail: concurrent callers
     * share one refresh, and a caller whose token was already replaced gets [RefreshOutcome.Refreshed]
     * without another one.
     */
    suspend fun refresh(staleAccessToken: String): RefreshOutcome

    /** The server keeps rejecting fresh tokens: tokens cleared, state [SessionState.Expired]. */
    suspend fun expire()
}
