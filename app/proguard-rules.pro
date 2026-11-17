# Models are (de)serialized by kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class space.aniroom.app.data.**$$serializer { *; }
-keepclassmembers class space.aniroom.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class space.aniroom.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit service interface (suspend functions)
-keep,allowobfuscation interface space.aniroom.app.data.AniApi
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# socket.io / engine.io
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
