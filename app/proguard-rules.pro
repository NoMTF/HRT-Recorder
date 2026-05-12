# Android author: Nanxin
# Release builds use R8/resource shrinking. Source code remains readable in the
# public repository; this protects distributed APK/AAB artifacts from casual
# repackaging rather than hiding the repository itself.

-keep class com.nanxin.hrtrecorder.MainActivity { *; }
-keep class com.nanxin.hrtrecorder.ReminderReceiver { *; }
-keep class com.nanxin.hrtrecorder.ReminderActionReceiver { *; }
-keep class com.nanxin.hrtrecorder.BootReceiver { *; }

-keepattributes SourceFile,LineNumberTable
