# Añadir reglas predeterminadas de ofuscación general
-dontwarn okio.**
-dontwarn okhttp3.**

# -------------------------------------------------------------
# REGLAS PARA KTOR Y MLKIT (Solución R8 Missing Classes)
# -------------------------------------------------------------
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
-dontwarn com.google.android.gms.internal.mlkit_entity_extraction.**

# -------------------------------------------------------------
# REGLAS PARA PDFBOX (Solución del error de compilación JP2Decoder)
# -------------------------------------------------------------
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** {
    public protected *;
}

# -------------------------------------------------------------
# REGLAS PARA GSON, ROOM Y FIREBASE (Data Classes)
# -------------------------------------------------------------
# Room
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }
-keep @androidx.room.Entity class *
-keep class * {
    @androidx.room.TypeConverter *;
}

# Gson
-dontwarn sun.misc.**
-keepattributes Signature, *Annotation*, EnclosingMethod
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }

# Firebase
# Las bibliotecas de Firebase ya incluyen sus propias reglas de ProGuard/R8
-dontwarn com.google.firebase.**

# Evita que se ofusquen los nombres de las clases y atributos de tus datos
# --- Reglas para el paquete antiguo (example) ---
-keep class com.example.finance_code.data.** { *; }
-keep class com.example.finance_code.chatbot.** { *; }

# --- Reglas para el paquete nuevo (help) ---
-keep class com.help.finance_code.data.** { *; }
-keep class com.help.finance_code.chatbot.** { *; }

# -------------------------------------------------------------
# REGLAS PARA MPAndroidChart
# -------------------------------------------------------------
-keep class com.github.mikephil.charting.**
-keepclassmembers class com.github.mikephil.charting.** {
    void set*(***);
    *** get*();
}

# -------------------------------------------------------------
# REGLAS PARA GOOGLE APIS Y DRIVE
# -------------------------------------------------------------
-keep class com.google.api.client.util.Key
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.client.util.GenericData { *; }
-keep class com.google.api.services.drive.model.** { *; }
