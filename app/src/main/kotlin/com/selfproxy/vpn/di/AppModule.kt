package com.selfproxy.vpn.di

import com.selfproxy.vpn.data.database.AppDatabase
import com.selfproxy.vpn.data.repository.AppRoutingRepository
import com.selfproxy.vpn.data.repository.ProfileRepositoryImpl
import com.selfproxy.vpn.data.repository.SettingsRepository
import com.selfproxy.vpn.domain.manager.AutoReconnectService
import com.selfproxy.vpn.domain.manager.ConnectionManager
import com.selfproxy.vpn.domain.manager.NetworkMonitor
import com.selfproxy.vpn.domain.manager.TrafficMonitor
import com.selfproxy.vpn.domain.manager.TrafficVerificationService
import com.selfproxy.vpn.domain.repository.CredentialStore
import com.selfproxy.vpn.domain.repository.ProfileRepository
import com.selfproxy.vpn.platform.security.AndroidCredentialStore
import com.selfproxy.vpn.platform.vless.VlessAdapter
import com.selfproxy.vpn.ui.viewmodel.AppRoutingViewModel
import com.selfproxy.vpn.ui.viewmodel.ConnectionViewModel
import com.selfproxy.vpn.ui.viewmodel.ProfileViewModel
import com.selfproxy.vpn.ui.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for the application.
 * Defines all dependencies and their lifecycles.
 * 
 * Organization:
 * 1. Database layer
 * 2. Repository layer
 * 3. Security layer
 * 4. Protocol adapters
 * 5. Domain services and managers
 * 6. ViewModels
 */
val appModule = module {
    
    // ========================================
    // Database Layer
    // ========================================
    
    /**
     * Room database instance (singleton).
     * Provides access to all DAOs.
     */
    single { AppDatabase.getInstance(androidContext()) }
    
    /**
     * Profile DAO for server profile operations.
     */
    single { get<AppDatabase>().profileDao() }
    
    /**
     * App routing DAO for per-app routing configuration.
     */
    single { get<AppDatabase>().appRoutingDao() }
    
    // ========================================
    // Repository Layer
    // ========================================
    
    /**
     * Profile repository for managing server profiles.
     * Requirements: 1.4, 1.5, 1.7, 1.8
     */
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    
    /**
     * Settings repository for app configuration.
     * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5
     */
    single { SettingsRepository(androidContext()) }
    
    /**
     * App routing repository for per-app VPN routing.
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
     */
    single { AppRoutingRepository(androidContext(), get()) }
    
    // ========================================
    // Security Layer
    // ========================================
    
    /**
     * Credential store for secure key storage using Android Keystore.
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.9, 2.10
     */
    single<CredentialStore> { 
        AndroidCredentialStore(androidContext()) 
    }
    
    // ========================================
    // Protocol Adapter
    // ========================================
    
    /**
     * VLESS protocol adapter.
     * Requirements: 3.2, 3.10, 7.8, 9.9-9.15
     */
    single { VlessAdapter(androidContext(), get()) }
    
    // ========================================
    // Domain Services and Managers
    // ========================================
    
    /**
     * Network monitor for detecting network changes.
     * Requirements: 6.5
     */
    single { NetworkMonitor(androidContext()) }
    
    /**
     * Traffic monitor for bandwidth tracking.
     * Requirements: 7.2, 7.3, 7.4, 7.5, 7.10
     */
    single { TrafficMonitor() }
    
    /**
     * Traffic verification service for IP and DNS leak detection.
     * Requirements: 8.9, 8.10
     */
    single { TrafficVerificationService() }
    
    /**
     * Connection manager for VPN connections.
     * Requirements: 3.3, 3.6, 3.7, 11.1, 11.3, 11.5
     */
    single { 
        ConnectionManager(
            vlessAdapter = get<VlessAdapter>(),
            profileRepository = get(),
            batteryOptimizationManager = get()
        )
    }
    
    /**
     * Auto-reconnect service for handling connection drops.
     * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.8, 6.9, 6.10
     * 
     * Note: Creates the service and sets up the bidirectional relationship with ConnectionManager.
     */
    single { 
        val connectionManager = get<ConnectionManager>()
        AutoReconnectService(androidContext(), connectionManager).also { autoReconnectService ->
            // Set up the bidirectional relationship
            connectionManager.setAutoReconnectService(autoReconnectService)
        }
    }
    
    /**
     * Battery optimization manager for power management.
     * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.7
     */
    single { com.selfproxy.vpn.domain.manager.BatteryOptimizationManager(androidContext()) }
    
    /**
     * Battery monitor for tracking battery state changes.
     * Requirements: 11.5, 11.6
     */
    single { com.selfproxy.vpn.domain.manager.BatteryMonitor(androidContext()) }
    
    /**
     * Notification manager for updating VPN service notifications.
     * Requirements: 3.5, 11.9
     */
    single { com.selfproxy.vpn.domain.manager.NotificationManager(androidContext(), get(), get()) }
    
    // ========================================
    // ViewModels
    // ========================================
    
    /**
     * Profile management ViewModel.
     * Requirements: 1.1, 1.2, 1.3, 1.5, 1.7, 1.8, 1.9
     */
    viewModel { ProfileViewModel(get(), get()) }
    
    /**
     * Connection screen ViewModel.
     * Requirements: 3.5, 7.1, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 8.9, 8.10
     */
    viewModel { ConnectionViewModel(get(), get(), get(), get()) }
    
    /**
     * Settings screen ViewModel.
     * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 11.2, 11.5
     */
    viewModel { SettingsViewModel(get(), get(), get()) }
    
    /**
     * App routing screen ViewModel.
     * Requirements: 5.1, 5.8
     */
    viewModel { AppRoutingViewModel(get()) }
}
