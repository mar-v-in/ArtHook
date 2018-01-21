# ArtHook

## Troubleshooting
In case an error like this happens:

More than one file was found with OS independent path 'lib/armeabi-v7a/libarthook_native.so'

Execute the following:

gradlew --stop
gradlew cleanBuildCache
gradlew clean