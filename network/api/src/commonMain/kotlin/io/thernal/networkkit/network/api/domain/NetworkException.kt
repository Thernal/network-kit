package io.thernal.networkkit.network.api.domain

/** What every [io.thernal.networkkit.network.api.data.client.ApiClient] call throws instead of a transport exception. */
class NetworkException(
    val error: NetworkError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
