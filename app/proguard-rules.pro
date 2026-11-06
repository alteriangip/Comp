# kotlinx.serialization: keep generated serializers for @Serializable classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.artcompanion.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.artcompanion.** {
    kotlinx.serialization.KSerializer serializer(...);
}
