# kotlinx.serialization keeps its generated serializers via its own consumer rules.
# Activities and receivers are referenced from the manifest only; R8 keeps those automatically.

# Google's ML Kit (the on-device model) finds its parts by name: the registrars listed in the merged manifest are
# made by reflection, through their constructor without arguments. Without this the release build shrinks those
# constructors away, the library comes up with no parts, and every prompt falls back to the Gemini app
# (found on the Lenovo with the release build of 2.0; the debug build never showed it).
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
