package io.thernal.networkkit.network.impl.data.cache

import io.thernal.networkkit.network.api.data.cache.CacheValidators
import io.thernal.networkkit.network.api.data.cache.HttpCacheStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Validators for this process only; every restart starts with unconditional requests. */
class InMemoryHttpCacheStore : HttpCacheStore {
    private val lock = Mutex()
    private val entries = mutableMapOf<String, CacheValidators>()

    override suspend fun get(key: String): CacheValidators? {
        return lock.withLock { entries[key] }
    }

    override suspend fun save(
        key: String,
        validators: CacheValidators,
    ) {
        lock.withLock { entries[key] = validators }
    }
}
