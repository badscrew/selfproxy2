package com.selfproxy.vpn.di

import com.selfproxy.vpn.data.database.AppDatabase
import com.selfproxy.vpn.data.repository.ProfileRepositoryImpl
import com.selfproxy.vpn.domain.manager.ConnectionManager
import com.selfproxy.vpn.domain.manager.TrafficMonitor
import com.selfproxy.vpn.domain.repository.ProfileRepository
import com.selfproxy.vpn.platform.vless.VlessAdapter
import com.selfproxy.vpn.platform.wireguard.WireGuardAdapter
import com.selfproxy.vpn.ui.viewmodel.ConnectionViewModel
import com.selfproxy.vpn.ui.viewmodel.ProfileViewModel
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
    
    // Repositories
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    
    // Protocol Adapters
    single { WireGuardAdapter() }
    single { VlessAdapter() }
    
    // Managers
    single { TrafficMonitor() }
    single { ConnectionManager(get(), get(), get()) }
    
    // ViewModels
    viewModel { ProfileViewModel(get()) }
    viewModel { ConnectionViewModel(get(), get(), get()) }
}
