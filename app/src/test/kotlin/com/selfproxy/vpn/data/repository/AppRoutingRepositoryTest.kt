package com.selfproxy.vpn.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.selfproxy.vpn.data.database.AppRoutingDao
import com.selfproxy.vpn.data.model.AppRoutingConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for AppRoutingRepository.
 * 
 * Tests:
 * - Routing configuration persistence
 * - Self-exclusion logic
 * 
 * Requirements: 5.7
 */
class AppRoutingRepositoryTest {
    
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var appRoutingDao: AppRoutingDao
    private lateinit var repository: AppRoutingRepository
    
    private val selfPackageName = "com.selfproxy.vpn"
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        appRoutingDao = mockk(relaxed = true)
        
        every { context.packageManager } returns packageManager
        every { context.packageName } returns selfPackageName
        
        // Mock getInstalledApplications to return empty list by default
        every { 
            packageManager.getInstalledApplications(any<Int>()) 
        } returns emptyList()
        
        repository = AppRoutingRepository(context, appRoutingDao)
    }

    
    @Test
    fun `saveConfig should persist configuration`() = runTest {
        // Arrange
        val config = AppRoutingConfig(
            profileId = 1L,
            routeAllApps = true,
            packageNames = setOf("com.example.app")
        )
        
        coEvery { appRoutingDao.insert(any()) } returns 1L
        
        // Act
        val result = repository.saveConfig(config)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { appRoutingDao.insert(any()) }
    }
    
    @Test
    fun `updateConfig should update existing configuration`() = runTest {
        // Arrange
        val config = AppRoutingConfig(
            id = 1L,
            profileId = 1L,
            routeAllApps = false,
            packageNames = setOf("com.example.app1", "com.example.app2")
        )
        
        coEvery { appRoutingDao.update(any()) } returns Unit
        
        // Act
        val result = repository.updateConfig(config)
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify { appRoutingDao.update(any()) }
    }
    
    @Test
    fun `getExcludedPackages should always include self app in route all mode`() = runTest {
        // Arrange
        val config = AppRoutingConfig(
            profileId = 1L,
            routeAllApps = true,
            packageNames = setOf("com.example.excluded")
        )
        
        coEvery { appRoutingDao.getByProfileId(1L) } returns config
        
        // Act
        val excluded = repository.getExcludedPackages(profileId = 1L)
        
        // Assert
        assertTrue(excluded.contains(selfPackageName), "Self app must always be excluded")
        assertTrue(excluded.contains("com.example.excluded"), "Explicitly excluded app should be excluded")
    }
    
    @Test
    fun `getExcludedPackages should always include self app in route selected mode`() = runTest {
        // Arrange
        val config = AppRoutingConfig(
            profileId = 1L,
            routeAllApps = false,
            packageNames = setOf("com.example.included")
        )
        
        coEvery { appRoutingDao.getByProfileId(1L) } returns config
        
        // Act
        val excluded = repository.getExcludedPackages(profileId = 1L)
        
        // Assert
        assertTrue(excluded.contains(selfPackageName), "Self app must always be excluded")
    }
    
    @Test
    fun `getIncludedPackages should never include self app`() = runTest {
        // Arrange
        val config = AppRoutingConfig(
            profileId = 1L,
            routeAllApps = true,
            packageNames = emptySet()
        )
        
        coEvery { appRoutingDao.getByProfileId(1L) } returns config
        
        // Act
        val included = repository.getIncludedPackages(profileId = 1L)
        
        // Assert
        assertFalse(included.contains(selfPackageName), "Self app must never be included")
    }
    
    @Test
    fun `getExcludedPackages should return self app on error`() = runTest {
        // Arrange
        coEvery { appRoutingDao.getByProfileId(any()) } throws Exception("Database error")
        coEvery { appRoutingDao.getGlobalConfig() } throws Exception("Database error")
        
        // Act
        val excluded = repository.getExcludedPackages(profileId = 1L)
        
        // Assert
        assertEquals(1, excluded.size)
        assertTrue(excluded.contains(selfPackageName), "Self app must be excluded even on error")
    }
    
    @Test
    fun `createDefaultConfig should create route all apps configuration`() {
        // Act
        val config = repository.createDefaultConfig(profileId = 1L)
        
        // Assert
        assertEquals(1L, config.profileId)
        assertTrue(config.routeAllApps)
        assertTrue(config.packageNames.isEmpty())
    }
}
