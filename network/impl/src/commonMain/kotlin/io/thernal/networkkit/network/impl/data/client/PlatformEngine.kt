package io.thernal.networkkit.network.impl.data.client

import io.ktor.client.engine.HttpClientEngine

/** OkHttp on Android, Darwin (NSURLSession) on iOS. */
internal expect fun platformEngine(): HttpClientEngine
