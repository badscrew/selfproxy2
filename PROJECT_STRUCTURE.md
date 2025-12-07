# SelfProxy VPN - Project Structure

## Overview

This document describes the project structure and organization of the SelfProxy VPN Android application.

## Root Directory

```
SelfProxy/
├── .kiro/                      # Kiro configuration and specs
│   ├── specs/selfproxy/        # Feature specifications
│   └── steering/               # Development guidelines
├── app/                        # Android application module
├── gradle/                     # Gradle wrapper files
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Gradle settings
├── gradle.properties           # Gradle properties
├── gradlew                     # Gradle wrapper (Unix)
├── gradlew.bat                 # Gradle wrapper (Windows)
├── .gitignore                  # Git ignore rules
├── README.md                   # Project README
└── PROJECT_STRUCTURE.md        # This file
```

## App Module Structure

```
app/
├── build.gradle.kts            # App module build configuration
├── proguard-rules.pro          # ProGuard/R8 rules for release builds
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── kotlin/com/selfproxy/vpn/
    │   │   ├── SelfProxyApplication.kt    # Application class
    │   │   ├── ui/                         # UI Layer (Jetpack Compose)
    │   │   │   ├── MainActivity.kt
    │   │   │   ├── theme/                  # Material 3 theme
    │   │   │   ├── screens/                # Screen composables (to be added)
    │   │   │   └── components/             # Reusable UI components (to be added)
    │   │   ├── domain/                     # Domain Layer (Business Logic)
    │   │   │   ├── model/                  # Domain models
    │   │   │   ├── repository/             # Repository interfaces
    │   │   │   └── adapter/                # Protocol adapter interface
    │   │   ├── data/                       # Data Layer
    │   │   │   ├── model/                  # Data models (Room entities)
    │   │   │   ├── repository/             # Repository implementations (to be added)
    │   │   │   └── database/               # Room database (to be added)
    │   │   ├── platform/                   # Platform Layer (Android-specific)
    │   │   │   ├── vpn/                    # VPN service
    │   │   │   ├── storage/                # Credential storage (to be added)
    │   │   │   └── network/                # Network monitoring (to be added)
    │   │   └── di/                         # Dependency Injection (Koin)
    │   │       └── AppModule.kt
    │   └── res/                            # Android resources
    │       ├── drawable/                   # Vector drawables
    │       ├── mipmap-*/                   # Launcher icons
    │       ├── values/                     # Strings, colors, themes
    │       └── xml/                        # XML configurations
    ├── test/                               # Unit tests
    │   └── kotlin/com/selfproxy/vpn/
    └── androidTest/                        # Instrumented tests
        └── kotlin/com/selfproxy/vpn/
```

## Architecture Layers

### UI Layer (`ui/`)
- **Purpose**: User interface using Jetpack Compose
- **Components**:
  - `MainActivity.kt`: Entry point
  - `theme/`: Material 3 theme configuration
  - `screens/`: Full-screen composables (to be added)
  - `components/`: Reusable UI components (to be added)
  - ViewModels (to be added)

### Domain Layer (`domain/`)
- **Purpose**: Business logic and use cases
- **Components**:
  - `model/`: Domain models (Protocol, ConnectionState, Connection)
  - `repository/`: Repository interfaces (ProfileRepository)
  - `adapter/`: Protocol adapter interface (ProtocolAdapter)
  - Use cases (to be added)

### Data Layer (`data/`)
- **Purpose**: Data persistence and management
- **Components**:
  - `model/`: Data models and Room entities (ServerProfile)
  - `repository/`: Repository implementations (to be added)
  - `database/`: Room database setup (to be added)

### Platform Layer (`platform/`)
- **Purpose**: Android-specific implementations
- **Components**:
  - `vpn/`: VPN service (TunnelVpnService)
  - `storage/`: Credential storage with Android Keystore (to be added)
  - `network/`: Network monitoring (to be added)
  - Protocol adapters (WireGuard, VLESS) (to be added)

### Dependency Injection (`di/`)
- **Purpose**: Koin dependency injection configuration
- **Components**:
  - `AppModule.kt`: Main DI module

## Key Technologies

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room 2.6.1
- **DI**: Koin 3.5.3
- **Async**: Kotlin Coroutines 1.7.3
- **Serialization**: kotlinx.serialization 1.6.2
- **Security**: Android Security Crypto 1.1.0-alpha06
- **VPN Protocols**:
  - WireGuard: wireguard-android 1.0.20230706
  - VLESS: AndroidLibXrayLite 1.8.5

## Build Configuration

### Gradle Files

1. **Root `build.gradle.kts`**:
   - Plugin versions
   - Common configuration

2. **`settings.gradle.kts`**:
   - Repository configuration
   - Module inclusion

3. **App `build.gradle.kts`**:
   - Dependencies
   - Build types (debug, release)
   - Compile options
   - ProGuard configuration

4. **`gradle.properties`**:
   - JVM arguments
   - Gradle optimization settings
   - Kotlin compiler settings

### ProGuard/R8 Rules

The `proguard-rules.pro` file contains rules for:
- Kotlin Serialization
- Room Database
- WireGuard library
- Xray-core (VLESS)
- OkHttp
- Koin
- Coroutines
- Jetpack Compose
- Android Security Crypto

## Testing Structure

### Unit Tests (`test/`)
- Location: `app/src/test/kotlin/`
- Purpose: Test business logic without Android dependencies
- Framework: JUnit 4, Kotest, MockK

### Instrumented Tests (`androidTest/`)
- Location: `app/src/androidTest/kotlin/`
- Purpose: Test Android-specific code on device/emulator
- Framework: AndroidX Test, Espresso, Compose UI Test

## Next Steps

The following components need to be implemented in subsequent tasks:

1. **Data Models and Database** (Task 2)
   - Complete Room database setup
   - Define all data models

2. **Profile Repository** (Task 3)
   - Implement ProfileRepository
   - Add CRUD operations

3. **Credential Store** (Task 4)
   - Android Keystore integration
   - Encryption/decryption

4. **Protocol Adapters** (Tasks 6-7)
   - WireGuard adapter
   - VLESS adapter

5. **Connection Manager** (Task 8)
   - Connection state management
   - Protocol selection

6. **VPN Service** (Task 9)
   - TUN interface management
   - Packet routing

7. **UI Screens** (Tasks 17-20)
   - Profile management
   - Connection screen
   - Settings
   - App routing

## Development Guidelines

- Follow Kotlin coding conventions
- Use Jetpack Compose for all UI
- Implement clean architecture principles
- Write tests for all business logic
- Use Koin for dependency injection
- Follow Material 3 design guidelines
- Ensure security best practices

## Resources

- [Android Developer Guide](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [WireGuard](https://www.wireguard.com/)
- [Xray-core](https://github.com/XTLS/Xray-core)
