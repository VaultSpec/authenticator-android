# Add project specific ProGuard rules here.
-keep class com.vaultspec.authenticator.data.model.** { *; }
-keep class com.vaultspec.authenticator.data.db.entity.** { *; }

# Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
