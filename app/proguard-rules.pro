# M11.1 R8/ProGuard 规则（Release 构建）
# 默认 proguard-android-optimize.txt 已包含基础规则；Room/Hilt 经 consumer rules 自带。

# ---- kotlinx-serialization（data:songs 导入校验 / data:local JSON 序列化）----
# 序列化器按 @Serializable 类生成，需保留其描述符与 serializer 入口（R8 会误删反射入口）
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Hilt/Dagger（官方 consumer rules 已覆盖；此处为防御性保留生成组件）----
-keep class dagger.hilt.** { *; }
-keep class matchsong.app.MatchSongApplication_HiltComponents* { *; }

# ---- Compose / Navigation / Room：框架自带 consumer rules，无需手写 ----
