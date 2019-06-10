# 概述
android常用编译功能插件，旨在把一些自动化的脚本收集在一起。

## 目前有以下功能：
* 设置settings.gradle中需要include的library源码路径
* settings.gradle中动态include项目
* 批量上传library到本地maven或者私有maven服务器
* 复制mapping.txt文件到指定目录
* 处理pin工程，pin工程概念建议参考这篇文章[微信Android模块化架构重构实践](https://www.jianshu.com/p/3990724aa7e4)

## settings.gradle
### 1. 设置settings.gradle中需要include的library源码路径
settings.gradle文件顶部添加
```
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:0.0.10'
    }
}
apply plugin: 'galaxybruce-pioneer-settings'

// LIBRARY_PATH是本地环境变量中配置的library路径，
// 第二个参数是打包机器上的library路径
def libraryPathWithKey = settings.ext.getLibraryPathWithKey('LIBRARY_PATH',
        '打包机器上的library路径')
```

### 2. settings.gradle中动态include项目
调用以下方法可以不用手动include library，会自动读取设置的目录下的所有library
```
// 读取工程根目录下的所有library并include进来，参数是不需要include的目录名称
settings.ext.includeDefaultModule(['lib1', 'lib2'])
// 读取libraryPathWithKey目录下的所有library并include进来，
// 参数是不需要include的目录名称
// 该方法可以多次调用或者第一个参数传多个目录
settings.ext.includeModule([libraryPathWithKey], ['aopstati','plugin'])
```

## build.gradle 
### 1. 批量上传library到本地maven或者私有maven服务器
rootproject中的build.gradle中添加如下代码
```
apply plugin: 'galaxybruce-pioneer'

galaxybrucepioneer {
    mavenScriptPath = 'maven上传脚本文件路径'
    // 需要批量上传到maven的library配置，具体格式可参考demo中的文件
    moduleDataPath = "${project.rootDir.path}/modulemaven.json"
}

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:0.0.10'
    }
}
```
PS: 每个library支持配置四个字段
name: 是需要上传到maven的library名称
artifactId: 如果不设置，默认是project.name
ver: 如果不设置，默认是android.defaultConfig.versionName
platform: 是否支持多平台的
```
{
  "group": "com.galaxybruce",
  "modules": [
    {"name": "testlibrary", "platform": true, "artifactId": "testlibrary", "ver": "0.0.1"}
  ]
}

```


### 2. 复制mapping.txt文件到指定目录
在app中的build.gradle添加

```
apply plugin: 'galaxybruce-pioneer'

// 是否开启copy mapping.txt功能
galaxybrucepioneer {
    copyMappingEnabled = true
    mappingDir = '目标copy目录'
}
```

### 3. 处理pin工程
在需要的pin工程module的build.gradle中添加即可
```
apply plugin: 'galaxybruce-pioneer'
```












