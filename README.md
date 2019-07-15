# 概述
android常用编译功能插件，旨在把一些自动化的脚本收集在一起。

## 目前有以下功能：
* 设置settings.gradle中需要include的library源码路径
* settings.gradle中动态include项目
* 批量上传library到本地maven或者私有maven服务器
* 复制mapping.txt文件到指定目录
* 处理pin工程，pin工程概念建议参考这篇文章[微信Android模块化架构重构实践](https://www.jianshu.com/p/3990724aa7e4)
* 多平台复用
* flutter module以及依赖的插件上传到maven

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
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:0.0.13'
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
// 第二个参数是不需要include的目录名称
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
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:0.0.13'
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

配置好以后，输入命令./gradlew uploadMaven即可

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
插件默认开启pin工程支持，在需要的pin工程module的build.gradle中添加即可，pin工程约定都已p_开头。
```
apply plugin: 'galaxybruce-pioneer'
```

### 4. 多平台复用
插件默认支持多平台复用，在需要开启多平台复用的module的build.gradle中添加即可。
```
apply plugin: 'galaxybruce-pioneer'
```
同时需要再项目根目录下的build.gradle指定当前平台资源所在目录
```
apply plugin: 'galaxybruce-pioneer'

galaxybrucepioneer {
    platformSourceDir = 'app2'
}
```
多平台项目结构：
![多平台项目结构](./images/mutil_platform.png)

### 5. flutter module以及依赖的插件上传到maven
在flutter module根目录下的gradle目录下添加文件root_build.gradle，文件内容如下
```
apply plugin: 'galaxybruce-pioneer'
galaxybrucepioneer {
    // 公司maven私服
    mavenUrl = 'http://172.172.177.240:8081/nexus/content/repositories/releases'
    // 公司maven私服SnapShot
    mavenUrlSnapShot = 'http://172.172.177.240:8081/nexus/content/repositories/snapshots'
    // maven账号
    mavenAccount = 'deployment'
    // maven密码
    mavenPwd = '666666'
    // true: 发布到本地仓库，false：发布到公司私服
    localMaven = false
}

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:3.2.1'
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:0.0.14'
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```
&emsp;
配合这篇文章[Flutter混编一键打包并上传maven](https://github.com/galaxybruce/galaxybruce.github.io/blob/master/flutter/Flutter%E6%B7%B7%E7%BC%96%E4%B8%80%E9%94%AE%E6%89%93%E5%8C%85%E5%B9%B6%E4%B8%8A%E4%BC%A0maven.md)中讲的AndroidFlutter.sh脚本，即可实现Flutter混编一键打包并上传maven









