package io.thernal.networkkit.network.api.data.cache

/** Keeps validators by key. `InMemoryHttpCacheStore` forgets them on restart; back it with storage to keep them. */
interface HttpCacheStore {
    suspend fun get(key: String): CacheValidators?

    suspend fun save(
        key: String,
        validators: CacheValidators,
    )
}
