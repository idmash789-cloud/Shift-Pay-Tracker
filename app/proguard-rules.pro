# R8 rules for the release build.
# Room: keep entities/DAOs and their fields.
-keep class com.shiftpay.tracker.data.** { *; }
-keepclassmembers class com.shiftpay.tracker.data.** { *; }
# AdMob / Play services: keep public APIs used via reflection.
-keep class com.google.android.gms.ads.** { *; }
