# TiendaTV Player ProGuard / R8 Rules
-keep class com.tiendatv.player.data.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
