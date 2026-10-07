package io.thernal.networkkit.network.impl.data.client

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Timeouts(
    val connect: Duration = 30.seconds,
    val request: Duration = 60.seconds,
    val socket: Duration = 60.seconds,
)
