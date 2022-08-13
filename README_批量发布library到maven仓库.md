## 简介
`批量发布library到maven仓库`支持根据配置把工程中多个module发布到maven仓库。

### 使用方式

#### 1. 根目录下的build.gradle中添加如下代码
```
apply plugin: 'galaxybruce-pioneer'

galaxybrucepioneer {
    // 配置需要发布到maven仓库的module信息
    moduleDataPath = "${project.rootDir.path}/modulemaven.json"
    
    // =================start==================
    // 公司maven私服
    mavenUrl = 'http://test.xxx.com/nexus/content/repositories/releases'
    // 公司maven私服SnapShot
    mavenUrlSnapShot = 'http://test.xxx.com/nexus/content/repositories/snapshots'
    // maven账号
    mavenAccount = 'deployment'
    // maven密码
    mavenPwd = '666666'
    // true: maven生成到指定目录：url = project.uri(project.rootProject.projectDir.absolutePath + '/repo-local')
    localMaven = false
    // =================end==================
}

buildscript {
    dependencies {
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:xxx'
    }
}
```

#### 2. 配置发布到module信息
modulemaven.json：
```
{
  "group": "com.galaxybruce",
  "version": "1.0.1",
  "modules": [
    {"name": "testlibrary", "artifactId": "testlibrary", "version": ""}
  ]
}
```
每个library支持配置三个字段:
* name: 必填项，必须是module名称
* artifactId: 可选项，优先级: artifactId > project.name
* version: 可选项，优先级: 这里的version > 上一层的version > android.defaultConfig.versionName


#### 3. 上传maven命令：
```
./gradlew uploadMaven
```

#### 4. 如果一个module上传为不同的平台发布，配置如下
modulemaven.json内容：
```
{
  "group": "com.galaxybruce",
  "version": "1.0.1",
  "platform_modules": {
    "app1": [
      {"name": "testlibrary"}
    ],
    "app2": [
      {"name": "testlibrary"}
    ]
  }
}
```

#### 5. 上传maven命令：
```
./gradlew uploadMaven -PplatformFlag=app1
./gradlew uploadMaven -PplatformFlag=app2
```








