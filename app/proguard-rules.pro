# ProGuard rules for release builds.

# Keep Room entities, DAOs, and database to avoid issues with obfuscation
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep our seed JSON model used by Gson reflection
-keepclassmembers class com.drivetheory.cbt.data.seed.QuestionSeedLoader$SeedQuestion { *; }
-keep class com.drivetheory.cbt.data.seed.QuestionSeedLoader$SeedQuestion

# Keep Kotlin metadata (helps with reflection/lambdas)
-keep class kotlin.Metadata { *; }

