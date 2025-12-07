package com.selfproxy.vpn

import android.app.Application
import com.selfproxy.vpn.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Main application class for SelfProxy VPN.
 * Initializes dependency injection and application-wide resources.
 */
class SelfProxyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin dependency injection
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SelfProxyApplication)
            modules(appModule)
        }
    }
}
