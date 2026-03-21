# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
# Hilt
-keep class dagger.hilt.** { *; }
# Kotlin Serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * { @kotlinx.serialization.Serializable *; }
# Keep data classes for Room
-keepclassmembers class com.mg.costeoapp.core.database.entity.** { *; }
-keepclassmembers class com.mg.costeoapp.core.database.dao.** { *; }
