package com.selfproxy.vpn.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.selfproxy.vpn.data.database.AppDatabase
import com.selfproxy.vpn.data.repository.AppRoutingRepository
import com.selfproxy.vpn.data.repository.SettingsRepository
import com.selfproxy.vpn.domain.manager.NetworkMonitor
import com.selfproxy.vpn.domain.manager.TrafficMonitor
import com.selfproxy.vpn.domain.repository.CredentialStore
import com.selfproxy.vpn.domain.repository.ProfileRepository
import com.selfproxy.vpn.ui.viewmodel.AppRoutingViewModel
import com.selfproxy.vpn.ui.viewmodel.ProfileViewModel
import com.selfproxy.vpn.ui.viewmodel.SettingsViewModel
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Koin dependency injection module.
 * 
 * Verifies that all dependencies are properly configured and can be resolved.
 * 
 * Note: Some tests are excluded because they require native libraries (WireGuard)
 * or have circular dependencies that are resolved at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppModuleTest : KoinTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        startKoin {
            androidContext(context)
            modules(appModule)
        }
    }
    
    @After
    fun teardown() {
        stopKoin()
    }
    
    /**
     * Verifies that Koin can be initialized with the app module.
     * This is the most important test - if Koin starts successfully,
     * the module configuration is valid.
     */
    @Test
    fun `koin should start successfully with app module`() {
        // If we get here, Koin started successfully
        assertNotNull(context)
    }
    
    // ========================================
    // Database Layer Tests
    // ========================================
    
    @Test
    fun `should provide AppDatabase instance`() {
        val database by inject<AppDatabase>()
        assertNotNull(database)
    }
    
    @Test
    fun `should provide ProfileDao instance`() {
        val database by inject<AppDatabase>()
        val profileDao = database.profileDao()
        assertNotNull(profileDao)
    }
    
    @Test
    fun `should provide AppRoutingDao instance`() {
        val database by inject<AppDatabase>()
        val appRoutingDao = database.appRoutingDao()
        assertNotNull(appRoutingDao)
    }
    
    // ========================================
    // Repository Layer Tests
    // ========================================
    
    @Test
    fun `should provide ProfileRepository instance`() {
        val repository by inject<ProfileRepository>()
        assertNotNull(repository)
    }
    
    @Test
    fun `should provide SettingsRepository instance`() {
        val repository by inject<SettingsRepository>()
        assertNotNull(repository)
    }
    
    @Test
    fun `should provide AppRoutingRepository instance`() {
        val repository by inject<AppRoutingRepository>()
        assertNotNull(repository)
    }
    
    // ========================================
    // Security Layer Tests
    // ========================================
    
    @Test
    fun `should provide CredentialStore instance`() {
        val credentialStore by inject<CredentialStore>()
        assertNotNull(credentialStore)
    }
    
    // ========================================
    // Domain Services Tests
    // ========================================
    
    @Test
    fun `should provide NetworkMonitor instance`() {
        val monitor by inject<NetworkMonitor>()
        assertNotNull(monitor)
    }
    
    @Test
    fun `should provide TrafficMonitor instance`() {
        val monitor by inject<TrafficMonitor>()
        assertNotNull(monitor)
    }
    
    // Note: ConnectionManager and AutoReconnectService tests are skipped
    // because they depend on WireGuard native libraries
    
    // ========================================
    // ViewModel Tests
    // ========================================
    
    @Test
    fun `should provide ProfileViewModel instance`() {
        val viewModel by inject<ProfileViewModel>()
        assertNotNull(viewModel)
    }
    
    // Note: ConnectionViewModel test is skipped because it depends on ConnectionManager
    
    @Test
    fun `should provide SettingsViewModel instance`() {
        val viewModel by inject<SettingsViewModel>()
        assertNotNull(viewModel)
    }
    
    @Test
    fun `should provide AppRoutingViewModel instance`() {
        val viewModel by inject<AppRoutingViewModel>()
        assertNotNull(viewModel)
    }
    
    // ========================================
    // Singleton Tests
    // ========================================
    
    @Test
    fun `database should be singleton`() {
        val database1 by inject<AppDatabase>()
        val database2 by inject<AppDatabase>()
        assertSame("Database should be singleton", database1, database2)
    }
    
    @Test
    fun `repositories should be singletons`() {
        val repo1 by inject<ProfileRepository>()
        val repo2 by inject<ProfileRepository>()
        assertSame("ProfileRepository should be singleton", repo1, repo2)
    }
    
    @Test
    fun `traffic monitor should be singleton`() {
        val monitor1 by inject<TrafficMonitor>()
        val monitor2 by inject<TrafficMonitor>()
        assertSame("TrafficMonitor should be singleton", monitor1, monitor2)
    }
}
