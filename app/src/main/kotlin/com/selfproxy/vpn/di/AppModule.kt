package com.selfproxy.vpn.di

import com.selfproxy.vpn.data.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection module for the application.
 * Defines all dependencies and their lifecycles.
 */
val appModule = module {
    // Database
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().profileDao() }
    
    // TODO: Add more dependencies as they are implemented
    // Example:
    // single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    // viewModel { ConnectionViewModel(get()) }
}
