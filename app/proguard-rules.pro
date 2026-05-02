# ─── PdfBox Android ──────────────────────────────────────────────────────────
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.**

# ─── OpenCV ──────────────────────────────────────────────────────────────────
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# ─── ML Kit text recognition (bundled) ──────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-dontwarn com.google.mlkit.**

# ─── ML Kit translate (bundled) ──────────────────────────────────────────────
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_translate.**

# ─── Room ────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep @androidx.room.Database class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static <fields>;
}

# ─── Kotlin Coroutines ───────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.coroutines.**

# ─── Kotlin Serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ─── Kotlin reflection (minimal) ─────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ─── Android general ────────────────────────────────────────────────────────
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Compose / Lifecycle ─────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ─── CameraX ─────────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ─── DocForge data/model classes ─────────────────────────────────────────────
-keep class com.docforge.core.domain.** { *; }
-keep class com.docforge.core.storage.db.** { *; }

# ─── Hilt / Dagger ───────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**
