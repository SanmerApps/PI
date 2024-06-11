-repackageclasses dev.sanmer.pi

# Keep DataStore fields
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* { <fields>; }