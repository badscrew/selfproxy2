package com.selfproxy.vpn.di

import com.selfproxy.vpn.data.database.AppDatabase
import com.selfproxy.vpn.data.repository.ProfileRepositoryImpl
import com.selfproxy.vpn.data.repository.SettingsRepository
import com.selfproxy.vpn.domain.manager.ConnectionManager
import com.selfproxy.vpn.domain.manager.TrafficMonitor
import com.selfproxy.vpn.domain.repository.ProfileRepository
import com.selfproxy.vpn.platform.vless.VlessAdapter
import com.selfproxy.vpn.platform.wireguard.WireGuardAdapter
import com.selfproxy.vpn.ui.viewmodel.ConnectionViewModel
import com.selfproxy.vpn.ui.viewmodel.ProfileViewModel
import com.selfproxy.vpn.ui.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for the application.
 * Defines all dependencies and their lifecycles.
 */
val appModule = module {
    // Database
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().profileDao() }
    single { get<AppDatabase>().appRoutingDao() }
    
    // Repositories
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single { SettingsRepository(androidContext()) }
    single { com.selfproxy.vpn.data.repository.AppRoutingRepository(androidContext(), get()) }
    
    // Security
    single<com.selfproxy.vpn.domain.repository.CredentialStore> { 
        com.selfproxy.vpn.platform.security.AndroidCredentialStore(androidContext()) 
    }
    
    // WireGuard Backend
    single<com.wireguard.android.backend.Backend> {
        com.wireguard.android.backend.GoBackend(androidContext())
    }
    
    // Protocol Adapters
    single { WireGuardAdapter(androidContext(), get(), get()) }
    single { VlessAdapter(androidContext(), get()) }
    
    // Managers
    single { TrafficMonitor() }
    single { ConnectionManager(get(), get(), get()) }
    
    // ViewModels
    viewModel { ProfileViewModel(get()) }
    viewModel { ConnectionViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { com.selfproxy.vpn.ui.viewmodel.AppRoutingViewModel(get()) }
}
