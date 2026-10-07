package io.thernal.networkkit.network.impl.data.retry

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
