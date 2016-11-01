# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/lens/.SDKs/android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn okhttp3.**
-dontnote okhttp3.**

-dontwarn com.squareup.picasso.**
-dontnote com.squareup.picasso.**

-dontnote org.sqlite.SQLite
-dontwarn org.sqlite.SQLite

-dontwarn de.qabel.**
-dontwarn java.nio.**

-dontwarn okio.**

-dontwarn sun.misc.Unsafe

-dontwarn org.jetbrains.anko.internals.AnkoInternals

-dontwarn org.spongycastle.**

-keep class org.spongycastle.**
