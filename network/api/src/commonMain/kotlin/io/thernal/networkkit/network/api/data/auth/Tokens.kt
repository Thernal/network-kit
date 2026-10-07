package io.thernal.networkkit.network.api.data.auth

/** An access token and the refresh token that renews it. */
data class Tokens(
    val accessToken: String,
    val refreshToken: String? = null,
)
