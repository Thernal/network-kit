package io.thernal.networkkit.network.impl.data.client

import io.thernal.networkkit.network.api.data.client.ErrorBodyParser
import io.thernal.networkkit.network.api.data.client.ResponseUnwrapper
import kotlinx.serialization.json.Json

/** What every client an `ApiClientFactory` makes shares. */
data class NetworkConfig(
    val json: Json = NetworkJson,
    val errorBodyParser: ErrorBodyParser = ErrorBodyParserImpl(json),
    val responseUnwrapper: ResponseUnwrapper? = null,
    /** Null installs no timeouts — for tests on virtual time, where a timeout fires at once. */
    val timeouts: Timeouts? = Timeouts(),
)
