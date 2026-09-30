package io.thernal.networkkit.network.impl.data.client

import io.thernal.networkkit.network.api.data.client.ErrorBodyParser
import io.thernal.networkkit.network.api.data.client.ResponseUnwrapper
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** What every client an `ApiClientFactory` makes shares. */
data class NetworkConfig(
    val json: Json = NetworkJson,
    val errorBodyParser: ErrorBodyParser = DefaultErrorBodyParser(json),
    val responseUnwrapper: ResponseUnwrapper? = null,
    /** Null installs no timeouts — for tests on virtual time, where a timeout fires at once. */
    val timeouts: Timeouts? = Timeouts(),
)

data class Timeouts(
    val connect: Duration = 30.seconds,
    val request: Duration = 60.seconds,
    val socket: Duration = 60.seconds,
)

/**
 * Lenient where servers usually are: unknown keys ignored, absent optional fields left at their
 * defaults, nulls not written.
 */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
