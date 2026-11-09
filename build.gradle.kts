// Root build file — plugin versions can be raised by Android Studio on first open.
// AGP 8.7.x + Kotlin 2.0.21 support compile/target SDK 36 (Android 16),
// which Google Play requires for new apps.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
