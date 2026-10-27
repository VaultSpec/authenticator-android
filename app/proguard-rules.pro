# Keep rules are deliberately narrow: anything kept here is excluded from R8 optimisation.

# Gson serialises these classes by reflection. Every field carries @SerializedName, so stored
# JSON (vault header, backups) does not depend on R8's renaming; this only stops their fields
# from being removed as unused.
-keep class com.vaultspec.authenticator.data.model.** { <fields>; }
-keep class com.vaultspec.authenticator.crypto.MasterKeyManager$* { <fields>; }

# Generic type information for Gson's TypeToken (Gson's bundled rules keep the rest)
-keepattributes Signature, *Annotation*

# Only Bouncy Castle's SCrypt is used, called directly, so the rest of it can be shrunk away
-dontwarn org.bouncycastle.**
-dontwarn sun.misc.**
