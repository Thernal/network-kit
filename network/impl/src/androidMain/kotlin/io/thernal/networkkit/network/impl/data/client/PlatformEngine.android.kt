package io.thernal.networkkit.network.impl.data.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun platformEngine(): HttpClientEngine {
    return OkHttp.create()
}
