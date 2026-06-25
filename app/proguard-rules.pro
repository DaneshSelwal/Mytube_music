# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Room entities
-keep class com.mark1.mytubemusic.data.model.** { *; }
-keepclassmembers class com.mark1.mytubemusic.data.model.** { *; }

# Keep Jsoup
-keep public class org.jsoup.** { *; }

# Keep Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
