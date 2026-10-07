package io.thernal.networkkit.network.impl.data.client

import kotlinx.serialization.json.Json

/**
 * Lenient where servers usually are: unknown keys ignored, absent optional fields left at their
 * defaults, nulls not written.
 */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
