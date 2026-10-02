package io.thernal.networkkit.network.impl.data.retry

import io.ktor.client.call.HttpClientCall
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.thernal.networkkit.network.api.data.connectivity.Connectivity
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import io.thernal.networkkit.network.api.data.interceptor.Interceptor
import kotlinx.coroutines.delay
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** How [RetryInterceptor] retries. */
data class RetryPolicy(
    /** Retries after the first attempt; 0 turns retrying off. */
    val maxRetries: Int = 2,
    val initialDelay: Duration = 500.milliseconds,
    val maxDelay: Duration = 8.seconds,
    /** A server's `Retry-After` longer than this is not waited for: the failure goes to the caller. */
    val maxRetryAfter: Duration = 30.seconds,
    /** Methods that are safe to send twice. POST and PATCH are not, and are never retried. */
    val methods: Set<HttpMethod> = setOf(
        HttpMethod.Get,
        HttpMethod.Head,
        HttpMethod.Put,
        HttpMethod.Delete,
        HttpMethod.Options,
    ),
    val statuses: Set<HttpStatusCode> = setOf(
        HttpStatusCode.TooManyRequests,
        HttpStatusCode.BadGateway,
        HttpStatusCode.ServiceUnavailable,
        HttpStatusCode.GatewayTimeout,
    ),
)

/**
 * Retries idempotent requests that failed for reasons a second try can fix: the connection dropped, a
 * timeout, the server unavailable or rate-limiting. Waits between tries grow exponentially with jitter,
 * or follow the server's `Retry-After`. While [connectivity] reports offline it does not retry at all —
 * the failure reaches the caller at once, as `NoConnection`.
 *
 * Installed after authentication (order 10 > 1), so each authenticated attempt is what gets retried.
 */
class RetryInterceptor(
    private val policy: RetryPolicy = RetryPolicy(),
    private val connectivity: ConnectivityMonitor? = null,
    private val random: Random = Random.Default,
) : Interceptor {
    override val order: Int = 10

    private val plugin: ClientPlugin<Unit> = createClientPlugin("ApiRetry") {
        on(Send) { request ->
            if (request.method !in policy.methods) {
                return@on proceed(request)
            }
            var attempt = 0
            var outcome = attemptOnce { proceed(request) }
            var wait = nextWait(outcome = outcome, attempt = attempt)
            while (wait != null) {
                delay(wait)
                attempt++
                outcome = attemptOnce { proceed(request) }
                wait = nextWait(outcome = outcome, attempt = attempt)
            }
            outcome.callOrThrow()
        }
    }

    override fun install(client: HttpClientConfig<*>) {
        client.install(plugin)
    }

    private fun nextWait(
        outcome: Outcome,
        attempt: Int,
    ): Duration? {
        if (attempt >= policy.maxRetries || isOffline()) {
            return null
        }
        return waitBefore(outcome = outcome, attempt = attempt)
    }

    private suspend fun attemptOnce(send: suspend () -> HttpClientCall): Outcome {
        return try {
            Outcome.Answered(send())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: IOException) {
            Outcome.Failed(failure)
        }
    }

    /** How long to wait before the next try, or null when [outcome] is final. */
    private fun waitBefore(
        outcome: Outcome,
        attempt: Int,
    ): Duration? {
        return when (outcome) {
            is Outcome.Failed -> backoff(attempt)

            is Outcome.Answered -> {
                val response = outcome.call.response
                if (response.status !in policy.statuses) {
                    return null
                }
                val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.seconds
                when {
                    retryAfter == null -> backoff(attempt)
                    retryAfter > policy.maxRetryAfter -> null
                    else -> retryAfter
                }
            }
        }
    }

    /** `initialDelay × 2^attempt`, capped, with ±25% jitter so clients that failed together do not retry together. */
    private fun backoff(attempt: Int): Duration {
        val base = (policy.initialDelay * (1 shl attempt.coerceAtMost(MAX_SHIFT))).coerceAtMost(policy.maxDelay)
        val jitter = 1.0 + (random.nextDouble() - HALF) * JITTER
        return base * jitter
    }

    private fun isOffline(): Boolean {
        return connectivity?.status?.value == Connectivity.Offline
    }

    private sealed interface Outcome {
        fun callOrThrow(): HttpClientCall

        data class Answered(
            val call: HttpClientCall,
        ) : Outcome {
            override fun callOrThrow(): HttpClientCall {
                return call
            }
        }

        data class Failed(
            val failure: IOException,
        ) : Outcome {
            override fun callOrThrow(): HttpClientCall {
                throw failure
            }
        }
    }

    private companion object {
        const val MAX_SHIFT = 16
        const val JITTER = 0.5
        const val HALF = 0.5
    }
}
