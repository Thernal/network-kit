package io.thernal.networkkit.network.api.domain

import kotlin.coroutines.cancellation.CancellationException

/** A call's outcome as a value: what `networkCall { }` returns. */
sealed interface NetworkResult<out T> {
    data class Success<T>(
        val value: T,
    ) : NetworkResult<T>

    data class Failure(
        val error: NetworkError,
    ) : NetworkResult<Nothing>
}

/**
 * Runs [block] — usually one or several `ApiClient` calls — and returns its value as
 * [NetworkResult.Success], or the [NetworkError] any call in it threw as [NetworkResult.Failure].
 * Cancellation is rethrown, never turned into a failure.
 */
suspend fun <T> networkCall(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: NetworkException) {
        NetworkResult.Failure(failure.error)
    }
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> {
    return when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(value))
        is NetworkResult.Failure -> this
    }
}

inline fun <T, R> NetworkResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (NetworkError) -> R,
): R {
    return when (this) {
        is NetworkResult.Success -> onSuccess(value)
        is NetworkResult.Failure -> onFailure(error)
    }
}

fun <T> NetworkResult<T>.getOrNull(): T? {
    return (this as? NetworkResult.Success)?.value
}
