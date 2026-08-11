# Apollo Kotlin 生成代码
-keep class com.apollographql.apollo3.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.modules.ApplicationContextModule { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# sshj
-dontwarn com.hierynomus.**

# Compose
-keep class androidx.compose.** { *; }
