# MOVA Phone - reglas R8 (Studio Lexair)
# El código de la app está totalmente en Kotlin/Compose: no hace falta conservar nada por reflexión
# salvo lo mínimo exigido por Android y Room.

-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Servicios y receptores declarados en el manifiesto (invocados por el sistema)
-keep class com.studiolexair.movaphone.services.** { *; }

# Eliminar logs de depuración en release (requisito de seguridad del proyecto)
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Coroutines / Kotlin
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**
-keepclassmembers class kotlin.Metadata { public <methods>; }
