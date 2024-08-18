#!/bin/bash
file="android-sdk-linux"
if [ ! -d "$file" ]
then
    wget --quiet --output-document=android-sdk.zip https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip
    unzip -q android-sdk.zip -d android-sdk-linux

    mkdir android-sdk-linux/licenses
    printf "8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e" > android-sdk-linux/licenses/android-sdk-license
    printf "84831b9409646a918e30573bab4c9c91346d8abd" > android-sdk-linux/licenses/android-sdk-preview-license
    android-sdk-linux/tools/bin/sdkmanager --update > update.log
    (echo y; echo y; echo y; echo y; echo y; echo y; echo y) | android-sdk-linux/tools/bin/sdkmanager "platforms;android-${ANDROID_COMPILE_SDK}" "build-tools;${ANDROID_BUILD_TOOLS}" "extras;google;m2repository" "extras;android;m2repository" "ndk;21.1.6352462" "cmake;3.6.4111459"
fi