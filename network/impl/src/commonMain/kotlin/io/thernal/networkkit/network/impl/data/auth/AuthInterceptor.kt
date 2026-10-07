package io.thernal.networkkit.network.impl.data.auth

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.thernal.networkkit.network.api.data.auth.AuthRoutes
import io.thernal.networkkit.network.api.data.auth.RefreshOutcome
import io.thernal.networkkit.network.api.data.auth.SessionManager
import io.thernal.networkkit.network.api.data.interceptor.Interceptor

/**
 * The HTTP half of authentication; the session half is [SessionManager]'s.
 *
 * - public routes: sent without `Authorization`;
 * - no token (a guest): sent without it; a 401 is the caller's answer — nothing to refresh or end;
 * - otherwise the token is attached, and a 401 asks the manager to refresh (one refresh however many
 *   requests failed together) and retries, at most [maxRetries] times. A refresh the server rejects has
 *   already expired the session; fresh tokens the server keeps rejecting expire it here;
 * - optional routes, when authentication fails for good, are retried once as a guest instead.
 */
class AuthInterceptor(
    private val session: SessionManager,
    routes: Set<AuthRoutes>,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
) : Interceptor {
    private val public = routes.flatMap(AuthRoutes::public)
    private val optional = routes.flatMap(AuthRoutes::optional)

    override val order: Int = 1

    private val plugin: ClientPlugin<Unit> = createClientPlugin("ApiAuth") {
        on(Send) { request ->
            if (request.matchesAny(public)) {
                request.headers.remove(HttpHeaders.Authorization)
                return@on proceed(request)
            }
            var token: String = session.accessToken() ?: run {
                request.headers.remove(HttpHeaders.Authorization)
                return@on proceed(request)
            }
            request.bearer(token)
            var call = proceed(request)
            var attempts = 0
            var hasRefreshed = false
            while (call.response.status == HttpStatusCode.Unauthorized && attempts < maxRetries) {
                attempts++
                if (session.refresh(staleAccessToken = token) != RefreshOutcome.Refreshed) {
                    break
                }
                token = session.accessToken() ?: break
                hasRefreshed = true
                request.bearer(token)
                call = proceed(request)
            }
            if (call.response.status != HttpStatusCode.Unauthorized) {
                return@on call
            }
            if (request.matchesAny(optional)) {
                request.headers.remove(HttpHeaders.Authorization)
                return@on proceed(request)
            }
            if (hasRefreshed && attempts >= maxRetries) {
                session.expire()
            }
            call
        }
    }

    override fun install(client: HttpClientConfig<*>) {
        client.install(plugin)
    }

    private fun HttpRequestBuilder.bearer(token: String) {
        headers[HttpHeaders.Authorization] = "Bearer $token"
    }

    private fun HttpRequestBuilder.matchesAny(
        patterns: List<io.thernal.networkkit.network.api.data.auth.ApiRoutePattern>,
    ): Boolean {
        val path = url.build().encodedPath
        return patterns.any { it.matches(method = method.value, path = path) }
    }
}

private const val DEFAULT_MAX_RETRIES = 3
