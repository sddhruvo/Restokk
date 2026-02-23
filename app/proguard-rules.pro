# ============================================================
# Home Inventory App — ProGuard / R8 Rules
# ============================================================

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ---- Gson ----
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep data classes used with Gson
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
# Gson TypeToken fix for R8 (Gson 2.10.1+) — preserves generic type info on anonymous TypeToken subclasses
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
# Keep app data classes deserialized by Gson
-keep class com.inventory.app.data.repository.FridgeItem { *; }
-keep class com.inventory.app.data.repository.ReceiptItem { *; }
-keep class com.inventory.app.data.local.entity.** { *; }
-keep class com.inventory.app.data.local.db.CategoriesData { *; }
-keep class com.inventory.app.data.local.db.CategorySeed { *; }
-keep class com.inventory.app.data.local.db.LocationsData { *; }
-keep class com.inventory.app.data.local.db.LocationSeed { *; }
-keep class com.inventory.app.data.local.db.UnitsData { *; }
-keep class com.inventory.app.data.local.db.UnitSeed { *; }
-keep class com.inventory.app.ui.screens.cook.SuggestedRecipe { *; }
-keep class com.inventory.app.ui.screens.cook.RecipeIngredient { *; }

# ---- Retrofit ----
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ---- Hilt / Dagger ----
-dontwarn dagger.internal.codegen.**
-keepclassmembers,allowobfuscation class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ---- Firebase ----
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ---- ML Kit ----
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# ---- CameraX ----
-keep class androidx.camera.** { *; }

# ---- Vico Charts ----
-keep class com.patrykandpatrick.vico.** { public protected *; }

# ---- Coil ----
-dontwarn coil.**

# ---- FuzzyWuzzy ----
-keep class me.xdrop.fuzzywuzzy.FuzzySearch { public *; }
-keep class me.xdrop.fuzzywuzzy.model.** { *; }
-keep class me.xdrop.fuzzywuzzy.ratios.** { *; }

# ---- Compose (R8 full mode) ----
-dontwarn androidx.compose.**

# ---- Coroutines ----
-dontwarn kotlinx.coroutines.**

# ---- General ----
-keepattributes SourceFile,LineNumberTable  # Better crash reports
-renamesourcefileattribute SourceFile
