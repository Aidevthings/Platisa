# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Kotlin & Coroutines ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.Transaction <methods>;
    @androidx.room.Ignore <methods>;
    @androidx.room.Ignore <fields>;
}

# --- Hilt / Dagger ---
-keep class com.example.platisa.PlatisaApplication_HiltComponents { *; }
-keep class com.example.platisa.di.** { *; }

# --- Retrofit / OkHttp / Gson ---
# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
# Keep data classes that are used for serialization/deserialization
-keep class com.example.platisa.core.data.model.** { *; }

# --- ML Kit / CameraX ---
# Usually R8 handles these well, but keeping them safe just in case
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }
-keep class com.google.mlkit.vision.text.** { *; }

# --- Timber ---
-dontwarn timber.log.Timber

# --- Compose ---
# R8 full mode is usually fine, but ensure these attributes are kept
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# --- PDFBox / Image decoding ---
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }