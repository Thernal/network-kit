package io.thernal.networkkit.network.impl.data.auth

import io.thernal.networkkit.network.api.data.auth.RefreshOutcome
import io.thernal.networkkit.network.api.data.auth.SessionManager
import io.thernal.networkkit.network.api.data.auth.SessionState
import io.thernal.networkkit.network.api.data.auth.TokenRefresher
import io.thernal.networkkit.network.api.data.auth.TokenStore
import io.thernal.networkkit.network.api.data.auth.Tokens
import io.thernal.networkkit.network.api.domain.NetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * [SessionManager] over the app's [TokenStore] and [TokenRefresher]. Tokens are read from the store
 * once and kept in memory — a secure store decrypts on every read — and written through on change.
 *
 * A refresh runs in [scope], not in the request that asked for it: a caller cancelled while waiting
 * does not cancel the refresh the other callers are waiting on too.
 */
class DefaultSessionManager(
    private val store: TokenStore,
    private val refresher: TokenRefresher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SessionManager {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Unknown)
    private val lock = Mutex()
    private var tokens: Tokens? = null
    private var isRestored = false
    private var inFlight: Deferred<RefreshOutcome>? = null

    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init {
        scope.launch { lock.withLock { restoreLocked() } }
    }

    override suspend fun accessToken(): String? {
        return lock.withLock {
            restoreLocked()
            tokens?.accessToken
        }
    }

    override suspend fun signIn(tokens: Tokens) {
        lock.withLock {
            store.write(tokens)
            this.tokens = tokens
            isRestored = true
            mutableState.value = SessionState.Authenticated
        }
    }

    override suspend fun signOut() {
        lock.withLock { endLocked(SessionState.Guest) }
    }

    override suspend fun expire() {
        lock.withLock { endLocked(SessionState.Expired) }
    }

    override suspend fun refresh(staleAccessToken: String): RefreshOutcome {
        val refresh = lock.withLock {
            restoreLocked()
            val current = tokens ?: return RefreshOutcome.Rejected
            if (current.accessToken != staleAccessToken) {
                return RefreshOutcome.Refreshed
            }
            inFlight ?: scope.async { renew(current) }.also { inFlight = it }
        }
        return refresh.await()
    }

    @Suppress("TooGenericExceptionCaught") // An app's refresher failing in any way keeps the session.
    private suspend fun renew(current: Tokens): RefreshOutcome {
        val outcome = try {
            val renewed = refresher.refresh(current)
            lock.withLock {
                if (renewed == null) {
                    endLocked(SessionState.Expired)
                    RefreshOutcome.Rejected
                } else {
                    store.write(renewed)
                    tokens = renewed
                    mutableState.value = SessionState.Authenticated
                    RefreshOutcome.Refreshed
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: NetworkException) {
            RefreshOutcome.Failed
        } catch (_: Exception) {
            RefreshOutcome.Failed
        }
        lock.withLock { inFlight = null }
        return outcome
    }

    private suspend fun restoreLocked() {
        if (isRestored) {
            return
        }
        tokens = store.read()
        isRestored = true
        mutableState.value = if (tokens == null) {
            SessionState.Guest
        } else {
            SessionState.Authenticated
        }
    }

    private suspend fun endLocked(next: SessionState) {
        store.clear()
        tokens = null
        isRestored = true
        mutableState.value = next
    }
}
