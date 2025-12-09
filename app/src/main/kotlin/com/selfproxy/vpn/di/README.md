# Dependency Injection with Koin

This directory contains the Koin dependency injection configuration for the SelfProxy VPN application.

## Overview

The application uses [Koin](https://insert-koin.io/) for dependency injection, which provides:
- Simple, lightweight DI framework for Kotlin
- No code generation or reflection
- Easy testing with mock/fake implementations
- Lifecycle-aware components (ViewModels)

## Module Structure

The `AppModule.kt` file defines all application dependencies organized into layers:

### 1. Database Layer
- **AppDatabase**: Room database singleton
- **ProfileDao**: Data access for server profiles
- **AppRoutingDao**: Data access for app routing configuration

### 2. Repository Layer
- **ProfileRepository**: Manages server profile CRUD operations
- **SettingsRepository**: Manages application settings
- **AppRoutingRepository**: Manages per-app routing configuration

### 3. Security Layer
- **CredentialStore**: Secure storage for VPN credentials using Android Keystore

### 4. Protocol Adapters
- **WireGuard Backend**: Native WireGuard implementation
- **WireGuardAdapter**: Protocol adapter for WireGuard connections
- **VlessAdapter**: Protocol adapter for VLESS connections

### 5. Domain Services
- **NetworkMonitor**: Monitors network state changes
- **TrafficMonitor**: Tracks bandwidth usage and connection statistics
- **ConnectionManager**: Manages VPN connections across protocols
- **AutoReconnectService**: Handles automatic reconnection on connection drops

### 6. ViewModels
- **ProfileViewModel**: Profile management screen
- **ConnectionViewModel**: Connection screen
- **SettingsViewModel**: Settings screen
- **AppRoutingViewModel**: App routing screen

## Usage

### In Application Class

The Koin module is initialized in `SelfProxyApplication.onCreate()`:

```kotlin
class SelfProxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SelfProxyApplication)
            modules(appModule)
        }
    }
}
```

### In Composables

ViewModels are injected using `koinViewModel()`:

```kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    // Use viewModel
}
```

### In Activities/Fragments

Dependencies can be injected using `by inject()`:

```kotlin
class MainActivity : ComponentActivity() {
    private val connectionManager: ConnectionManager by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use connectionManager
    }
}
```

### In Services

Dependencies can be injected in Android Services:

```kotlin
class TunnelVpnService : VpnService() {
    private val connectionManager: ConnectionManager by inject()
    
    override fun onCreate() {
        super.onCreate()
        // Use connectionManager
    }
}
```

## Dependency Scopes

### Singleton (`single`)
Used for components that should have only one instance throughout the app lifecycle:
- Database
- Repositories
- Managers (ConnectionManager, TrafficMonitor, etc.)
- Protocol adapters

### ViewModel (`viewModel`)
Used for ViewModels that are lifecycle-aware and scoped to their owner:
- ProfileViewModel
- ConnectionViewModel
- SettingsViewModel
- AppRoutingViewModel

## Circular Dependency Handling

The `ConnectionManager` and `AutoReconnectService` have a circular dependency:
- ConnectionManager needs AutoReconnectService to enable auto-reconnection
- AutoReconnectService needs ConnectionManager to trigger reconnections

This is resolved using setter injection:

```kotlin
single { 
    ConnectionManager(get(), get(), get()).apply {
        setAutoReconnectService(get())
    }
}
```

## Testing

### Unit Tests

For unit tests, you can use Koin's test utilities:

```kotlin
class MyTest : KoinTest {
    @Before
    fun setup() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(appModule)
        }
    }
    
    @After
    fun teardown() {
        stopKoin()
    }
    
    @Test
    fun `test with injected dependency`() {
        val repository by inject<ProfileRepository>()
        // Test with repository
    }
}
```

### Mocking Dependencies

For tests that need mocked dependencies, create a test module:

```kotlin
val testModule = module {
    single<ProfileRepository> { mockk<ProfileRepository>() }
}

startKoin {
    modules(testModule)
}
```

## Adding New Dependencies

To add a new dependency:

1. **Add to AppModule.kt** in the appropriate section:

```kotlin
// In the appropriate section
single { MyNewService(get(), get()) }
```

2. **Use in your code**:

```kotlin
class MyClass {
    private val myService: MyNewService by inject()
}
```

3. **Add tests** in `AppModuleTest.kt`:

```kotlin
@Test
fun `should provide MyNewService instance`() {
    val service by inject<MyNewService>()
    assertNotNull(service)
}
```

## Best Practices

### Do's ✅
- Use `single` for stateless services and repositories
- Use `viewModel` for ViewModels
- Inject interfaces, not concrete implementations
- Keep dependencies minimal (avoid injecting everything)
- Document complex dependency relationships
- Test that all dependencies can be resolved

### Don'ts ❌
- Don't create circular dependencies (use setter injection if needed)
- Don't inject Android Context directly (use `androidContext()`)
- Don't use `get()` outside of Koin module definitions
- Don't create multiple Koin instances
- Don't forget to call `stopKoin()` in tests

## Troubleshooting

### "No definition found for..."

This error means a dependency is not defined in the module. Check:
1. Is the dependency defined in `appModule`?
2. Is the type correct (interface vs implementation)?
3. Are all transitive dependencies defined?

### "Circular dependency detected"

Use setter injection or restructure your dependencies to break the cycle.

### "Cannot resolve parameter..."

Check that all constructor parameters of your class are defined in Koin.

## Resources

- [Koin Documentation](https://insert-koin.io/)
- [Koin for Android](https://insert-koin.io/docs/reference/koin-android/start)
- [Koin Testing](https://insert-koin.io/docs/reference/koin-test/testing)
