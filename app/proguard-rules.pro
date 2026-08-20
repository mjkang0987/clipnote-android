# kotlinx.serialization — @Serializable 이 만든 serializer 를 리플렉션으로 찾는다.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kr.co.clipnote.** {
    *** Companion;
}
-keepclasseswithmembers class kr.co.clipnote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
