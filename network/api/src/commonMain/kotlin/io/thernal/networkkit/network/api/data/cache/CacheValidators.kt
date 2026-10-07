package io.thernal.networkkit.network.api.data.cache

/** A response's validators: what a conditional request sends back as `If-None-Match` / `If-Modified-Since`. */
data class CacheValidators(
    val etag: String?,
    val lastModified: String?,
)
