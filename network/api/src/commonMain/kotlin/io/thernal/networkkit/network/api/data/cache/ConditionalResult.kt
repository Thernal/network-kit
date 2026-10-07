package io.thernal.networkkit.network.api.data.cache

sealed interface ConditionalResult<out T> {
    data class Modified<T>(
        val value: T,
        val validators: CacheValidators,
    ) : ConditionalResult<T>

    data object NotModified : ConditionalResult<Nothing>
}
