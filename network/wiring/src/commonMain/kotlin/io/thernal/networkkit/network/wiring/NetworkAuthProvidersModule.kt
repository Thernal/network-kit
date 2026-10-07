package io.thernal.networkkit.network.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.networkkit.network.api.data.auth.AuthRoutes
import io.thernal.networkkit.network.api.data.auth.SessionManager
import io.thernal.networkkit.network.api.data.auth.TokenRefresher
import io.thernal.networkkit.network.api.data.auth.TokenStore
import io.thernal.networkkit.network.api.data.interceptor.Interceptor
import io.thernal.networkkit.network.impl.data.auth.AuthInterceptor
import io.thernal.networkkit.network.impl.data.auth.SessionManagerImpl

/**
 * Authentication: the [SessionManager] and the auth interceptor. The app binds [TokenStore] and
 * [TokenRefresher], and contributes [AuthRoutes] — its public routes (the refresh endpoint among them)
 * and optional ones. An app without sign-in excludes this container from its graph.
 *
 * The refresher reaches its endpoint through an `ApiClient`, whose interceptors include this one, which
 * needs the session manager: the manager takes the refresher lazily to break that cycle.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface NetworkAuthProvidersModule {
    @Multibinds(allowEmpty = true)
    val authRoutes: Set<AuthRoutes>

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideSessionManager(
            store: TokenStore,
            refresher: Lazy<TokenRefresher>,
        ): SessionManager {
            return SessionManagerImpl(store = store, refresher = { current -> refresher.value.refresh(current) })
        }

        @Provides
        @IntoSet
        fun provideAuthInterceptor(
            session: SessionManager,
            routes: Set<AuthRoutes>,
        ): Interceptor {
            return AuthInterceptor(session = session, routes = routes)
        }
    }
}
