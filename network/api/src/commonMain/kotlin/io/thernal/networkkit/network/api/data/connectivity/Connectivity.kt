package io.thernal.networkkit.network.api.data.connectivity

enum class Connectivity {
    /** Not known yet — the platform has not reported. Treat it as online. */
    Unknown,

    /** A network with internet access. */
    Online,

    /** No network, or one without internet access. */
    Offline,
}
