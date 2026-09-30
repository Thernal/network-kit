package io.thernal.networkkit.network.impl.data.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun platformEngine(): HttpClientEngine {
    return Darwin.create()
}
