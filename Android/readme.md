
## 集成

Project build.gradle：
```kotlin
allprojects {
    repositories {
        maven {
            url 'http://maven.faceunity.com/repository/maven-public/'
            allowInsecureProtocol true
        }
    }
}
```
Model build.gradle:
```kotlin
dependencies {
    //核心渲染库（RenderKit）
    implementation "com.faceunity.gpb:core-scene:2.1.0"
    //便捷开发库（AvatarFactory）
    implementation "com.faceunity.gpb:avatar-factory:1.5.0"
}
```
此处版本视实际情况集成。

proguard-rules.pro：
```kotlin
-keep class com.faceunity.** {*;}
```

## 资源
以下资源为 RenderKit 所需，必须与其版本严格一致。

**EngineAssets.bundle**： 形象渲染资源

**ai_face_processor_e47_s1.bundle**： 人脸驱动模型

**ai_human_processor.bundle**： 身体驱动模型


更多内容参考 Demo 中的集成代码。