# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
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

# only peephole
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!class/merging/*
-optimizationpasses 5
-dontpreverify

#-keepnames class de.dakror.** {*;}
#-keepnames class com.badlogic.** {*;}


-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-flattenpackagehierarchy

-dontwarn android.support.**
-dontwarn com.android.tools.profiler.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild

-keep class androidx.inspection.** { *; }
-dontwarn androidx.inspection.**


# keep all structure constructors for the reflective version check
-keepclassmembers class de.dakror.quarry.structure.** {
   public <init>(...);
}

-keep class com.badlogic.gdx.controllers.android.AndroidControllers

-keep class com.badlogic.gdx.graphics.Color
-keep class com.badlogic.gdx.graphics.g2d.BitmapFont
-keepclasseswithmembers class com.badlogic.gdx.scenes.** {
	<fields>;
}

-keepclassmembernames class de.dakror.quarry.** extends java.lang.Enum {
    <fields>;
}


-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
   <init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);
}

-keepclassmembers class com.badlogic.gdx.physics.box2d.World {
   boolean contactFilter(long, long);
   void    beginContact(long);
   void    endContact(long);
   void    preSolve(long, long);
   void    postSolve(long, long);
   boolean reportFixture(long);
   float   reportRayFixture(long, float, float, float, float, float);
}

-keepclassmembers class de.dakror.quarry.game.Item$Element {
    <fields>;
}

-keepclassmembers public class * extends de.dakror.quarry.structure.base.Structure {
   public <init>(int, int);
}

-keep class de.dakror.quarry.game.LoadingCompat {*;}

-keepclassmembers class de.dakror.quarry.ui.* {
    public <init>(...);
}

# Keep all members with Autumn annotations:
-keepclassmembers,allowobfuscation class * {
    @com.github.czyzby.** *;
}

# Required LibGDX config changes:
-keepclassmembers enum com.badlogic.gdx.*$* { <fields>; }

-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

-keep class net.jpountz.** {*;}
-dontwarn net.jpountz.**


# default google

# The remainder of this file is identical to the non-optimized version
# of the Proguard configuration file (except that the other file has
# flags to turn off optimization).
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-keepattributes *Annotation*
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService
# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}
# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}
# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}
# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**