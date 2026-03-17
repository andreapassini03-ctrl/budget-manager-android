# Aggiungi questa regola per le tue classi di dati (models)
# Sostituisci il nome del pacchetto con il tuo.
-keepclassmembers class com.budgetapp.budgetapp.data.** {
    <init>();
}

# In alternativa, se non vuoi offuscare le tue classi di dati
# questo mantiene anche i nomi dei campi e dei metodi.
-keep public class com.budgetapp.budgetapp.data.** { public *; }

# Preserva attributi importanti (annotazioni, generics, eccezioni)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses,EnclosingMethod

# Queste regole sono necessarie per Firestore in generale.
-keep class com.google.firebase.firestore.** { *; }
-keep interface com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**

# Se usi gli snapshot (onSnapshot), aggiungi anche questo
-keep class com.google.firebase.firestore.core.** { *; }
-keep class com.google.firebase.firestore.remote.** { *; }
-keep class com.google.firebase.firestore.model.** { *; }
-keep class com.google.firebase.firestore.util.** { *; }

# Regole per Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep interface com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Regole per Firebase UI Auth (se usata)
-keep class com.firebase.ui.** { *; }
-dontwarn com.firebase.ui.**

# Regole per Google Play Services / Google Sign-In
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Mantieni le classi SafeParcelable (usate da Play Services)
-keep class * implements com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final android.os.Parcelable$Creator *;
}

# Mantieni Kotlin metadata per riflessione
-keep class kotlin.Metadata { *; }

# Assicurati che le classi interne di Firebase Auth non vengano rimosse/obfuscate
-keep class com.google.android.gms.internal.firebase-auth-api.** { *; }
-keep class com.google.firebase.components.** { *; }
-keep class com.google.firebase.provider.** { *; }
