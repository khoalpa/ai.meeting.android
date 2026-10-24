# Native/ML runtimes expose entry points through JNI and reflection.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.llamatik.** { *; }
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keep class org.vosk.** { *; }

# ML Kit and Google Tasks include consumer rules, but keeping public model APIs
# avoids release-only reflection surprises around on-device downloads.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Apache POI reads Word documents via reflection-heavy parsers.
-keep class org.apache.poi.** { *; }
-keep class org.apache.commons.compress.** { *; }

# Optional desktop/codec integrations referenced by POI and commons-compress are
# not packaged on Android. The app only uses the basic Word and tar.bz2 paths.
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn javax.xml.stream.**
-dontwarn org.brotli.dec.**
-dontwarn org.jspecify.annotations.**
-dontwarn org.objectweb.asm.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**
