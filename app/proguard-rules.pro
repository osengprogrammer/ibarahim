# 1. Native C++ (JNI) - CRITICAL for ISO Key
# Mencegah ProGuard merubah nama fungsi yang terhubung ke C++
-keepclasseswithmembernames class * {
    native <methods>;
}

# Jaga agar class NativeKeyStore tidak di-rename
-keep class com.example.crashcourse.util.NativeKeyStore { *; }

# 2. ML Kit & Face Detection
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# 3. TensorFlow Lite (TFLite)
# Mencegah error "Native method not found" saat scanning wajah
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# 4. Firebase & Firestore
# Mencegah class model data terhapus saat parsing data dari cloud
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# 5. Room Database
# Mencegah error pada tabel database lokal
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.crashcourse.db.** { *; }

# 6. Serialization (GSON)
# Dibutuhkan agar JSON parsing tetap jalan
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 7. Preserve Line Numbers for Debugging
# Agar kalau ada crash, logcat tetap menunjukkan baris error yang asli
-keepattributes SourceFile,LineNumberTable
# Fix untuk Error SLF4J (Missing StaticLoggerBinder)
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Fix tambahan untuk itext dan opencsv agar tidak error saat export PDF/CSV
-dontwarn com.itextpdf.**
-dontwarn com.opencsv.**
-dontwarn org.bouncycastle.**

# Jika ada error terkait javax atau java.beans
-dontwarn java.beans.**
-dontwarn javax.annotation.**