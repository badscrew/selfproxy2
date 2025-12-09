# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signature for reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ================================
# Kotlin Serialization
# ================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializers
-keep,includedescriptorclasses class com.selfproxy.vpn.**$$serializer { *; }
-keepclassmembers class com.selfproxy.vpn.** {
    *** Companion;
}
-keepclasseswithmembers class com.selfproxy.vpn.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep serialization runtime
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serialization descriptors
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# ================================
# Room Database
# ================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Keep Room DAO methods
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract ** *Dao();
}

# Keep Room type converters
-keep class * {
    @androidx.room.TypeConverter <methods>;
}

# Keep Room database implementations
-keep class com.selfproxy.vpn.data.database.** { *; }

# ================================
# WireGuard Library
# ================================
# Keep all WireGuard classes and native methods
-keep class com.wireguard.** { *; }
-keepclassmembers class com.wireguard.** { *; }
-keepclasseswithmembernames class com.wireguard.** {
    native <methods>;
}

# Keep WireGuard config classes
-keep class com.wireguard.config.** { *; }
-keep class com.wireguard.crypto.** { *; }

# Keep WireGuard backend
-keep class com.wireguard.android.backend.** { *; }

# Prevent obfuscation of WireGuard tunnel interface
-keep interface com.wireguard.android.backend.Tunnel { *; }
-keep class * implements com.wireguard.android.backend.Tunnel { *; }

# ================================
# Xray-core (VLESS) Library
# ================================
# Keep all Xray/V2Ray classes
-keep class io.github.2dust.** { *; }
-keepclassmembers class io.github.2dust.** { *; }
-keep class libv2ray.** { *; }
-keepclassmembers class libv2ray.** { *; }

# Keep native methods for Xray
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Xray configuration classes
-keep class io.github.2dust.AndroidLibXrayLite.** { *; }

# Suppress warnings for Xray
-dontwarn io.github.2dust.**
-dontwarn libv2ray.**

# ================================
# OkHttp & Networking
# ================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep OkHttp platform used only on JVM
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# OkHttp platform used only on JVM and when Conscrypt dependency is available
-dontwarn org.codehaus.mojo.animal_sniffer.*

# Keep OkHttp interceptors
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ================================
# Koin Dependency Injection
# ================================
-keep class org.koin.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Koin modules
-keep class * extends org.koin.core.module.Module { *; }

# Keep classes with Koin annotations
-keep @org.koin.core.annotation.* class * { *; }

# ================================
# Kotlin Coroutines
# ================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep coroutines volatile fields
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep coroutines debug info
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ================================
# Jetpack Compose
# ================================
# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Compose compiler generated classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ================================
# Application Data Models
# ================================
# Keep all data classes
-keep class com.selfproxy.vpn.data.model.** { *; }
-keepclassmembers class com.selfproxy.vpn.data.model.** { *; }

# Keep domain models
-keep class com.selfproxy.vpn.domain.model.** { *; }
-keepclassmembers class com.selfproxy.vpn.domain.model.** { *; }

# Keep data classes with all fields
-keepclassmembers class com.selfproxy.vpn.data.** {
    <fields>;
    <init>(...);
}

# ================================
# ViewModels
# ================================
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep ViewModel factory
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ================================
# VPN Service
# ================================
# Keep VPN Service and all methods
-keep class * extends android.net.VpnService {
    <init>(...);
    public *;
    protected *;
}

# Keep our VPN service implementation
-keep class com.selfproxy.vpn.platform.vpn.TunnelVpnService { *; }

# ================================
# Android Security & Crypto
# ================================
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** { *; }

# Keep credential store
-keep class com.selfproxy.vpn.platform.security.** { *; }

# Keep Android Keystore
-keep class android.security.keystore.** { *; }

# ================================
# Protocol Adapters
# ================================
# Keep protocol adapter interfaces and implementations
-keep interface com.selfproxy.vpn.domain.adapter.** { *; }
-keep class com.selfproxy.vpn.platform.wireguard.** { *; }
-keep class com.selfproxy.vpn.platform.vless.** { *; }

# ================================
# Repositories
# ================================
-keep interface com.selfproxy.vpn.domain.repository.** { *; }
-keep class com.selfproxy.vpn.data.repository.** { *; }

# ================================
# Managers & Services
# ================================
-keep class com.selfproxy.vpn.domain.manager.** { *; }

# ================================
# ML Kit (QR Code Scanning)
# ================================
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ================================
# CameraX
# ================================
-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { *; }

# ================================
# DataStore
# ================================
-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.** { *; }

# ================================
# Enum Classes
# ================================
# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================================
# Parcelable
# ================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ================================
# Native Methods
# ================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ================================
# Reflection
# ================================
# Keep classes accessed via reflection
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ================================
# Suppress Warnings
# ================================
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn javax.naming.**

# ================================
# Optimization
# ================================
# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ================================
# Debugging
# ================================
# Uncomment for debugging ProGuard issues
# -printconfiguration proguard-config.txt
# -printusage proguard-usage.txt
# -printmapping proguard-mapping.txt


# ================================
# Google Tink (ML Kit dependency)
# ================================
# Suppress warnings for optional Tink dependencies
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**
-dontwarn com.google.crypto.tink.util.KeysDownloader

# Keep Tink classes if present
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
