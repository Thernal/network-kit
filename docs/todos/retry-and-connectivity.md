# Retry policy and connectivity

**Status:** open

Neither source app retried idempotent requests on `NoConnection`/`Timeout`/`Unavailable`, or observed
connectivity. Both are app-level policies today (an `Interceptor`, a repository). Decide whether the kit
ships a bounded, idempotent-only retry interceptor with backoff, and whether it offers a connectivity
`Flow` (Android `ConnectivityManager`, iOS `NWPathMonitor`) — the latter would be the kit's first platform
code outside the engines.
