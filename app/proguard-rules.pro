# lunar-java uses reflection-free POJOs, but keep it safe for the release build.
-keep class com.nlf.calendar.** { *; }
-keep class com.google.gson.** { *; }

# Gson DTOs in the data layer are instantiated reflectively.
-keep class com.dotcalendar.app.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
