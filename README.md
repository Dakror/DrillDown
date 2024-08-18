# Drill Down

This is the open-sourced codebase of the factory building game Drill Down, released in 2019 on Steam and Google Play. 

Binaries for Android and PC are available at [itch.io](https://dakror.itch.io/drill-down).

## Building from source

The game needs 64-bit Java >= 11 and Android Studio to compile.

1. Create a java keystore for code signing
2. Enter the credentials into `gradle.properties`
3. For Android run `gradle android:assembleFullRelease`, for PC run `desktop:dist` to get the runnable binaries
