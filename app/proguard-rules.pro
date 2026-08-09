# NexaSense — R8 rules for the release build
# --------------------------------------------
# The app uses no reflection, no runtime class lookup, and no custom
# serialization, so R8's default Android rules plus the consumer rules
# shipped by AndroidX (Compose, Navigation, DataStore, coroutines,
# profileinstaller) are sufficient to shrink and obfuscate safely.
#
# The rules below only make release crash reports readable. If a future
# feature adds reflection, serialization or resource lookup, add its keep
# rules here.

# Keep line numbers so Play Console / bug-report stack traces map back to
# source lines even though names are obfuscated.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
