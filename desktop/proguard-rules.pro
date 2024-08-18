#-optimizations !field/*,!class/merging/*
#-dontoptimize
-dontnote

-optimizationpasses 3

-printmapping build/libs/mapping.txt

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-flattenpackagehierarchy


# keep all structure constructors for the reflective version check
-keepclassmembers class de.dakror.quarry.structure.** {
   public <init>(...);
}

-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild
-dontwarn org.lwjgl.**

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class org.lwjgl.** {*;}
-keep class com.badlogic.** {*;}
-keep class javax.crypto.** {*;}


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

-keepclasseswithmembers public class * {
	public static void main(java.lang.String[]);
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

-verbose
-keepattributes *Annotation*

-keepclasseswithmembernames class * {
    native <methods>;
}
