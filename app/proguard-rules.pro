# Añadir reglas predeterminadas de ofuscación general
-dontwarn okio.**
-dontwarn okhttp3.**

# -------------------------------------------------------------
# REGLAS PARA PDFBOX (Solución del error de compilación JP2Decoder)
# -------------------------------------------------------------
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }

# -------------------------------------------------------------
# REGLAS PARA GSON, ROOM Y FIREBASE (Data Classes)
# -------------------------------------------------------------
# Evita que se ofusquen los nombres de las clases y atributos de tus datos
# Esto es vital para que al leer y escribir en Firestore o Room no dé NullPointer

# --- Reglas para el paquete antiguo (example) ---
-keep class com.example.finance_code.data.** { *; }
-keep class com.example.finance_code.chatbot.** { *; }

# --- Reglas para el paquete nuevo (help) ---
-keep class com.help.finance_code.data.** { *; }
-keep class com.help.finance_code.chatbot.** { *; }

# Mantener la información de los atributos genéricos (Listas, HashMaps, etc.)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# -------------------------------------------------------------
# REGLAS PARA MPAndroidChart
# -------------------------------------------------------------
-keep class com.github.mikephil.charting.** { *; }
-keep class com.github.mikephil.charting.data.** { *; }

# -------------------------------------------------------------
# REGLAS PARA GOOGLE APIS Y DRIVE
# -------------------------------------------------------------
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }