## bintray-release  [![Download](https://img.shields.io/badge/version-0.0.26-blue.svg)](https://bintray.com/galaxybruce/maven/pioneer-gradle-plugin/_latestVersion) [![](./assets/btn_apache_lisence.png)](LICENSE)
## 目前有以下功能：
* settings.gradle中从本地环境变量或者local.propertie中读取需要include的library源码路径
* settings.gradle中智能识别android moduel并include进来
* settings.gradle或者build.gradle中读取local.properties中的值
* 批量上传library到本地maven或者私有maven服务器
* 处理pin工程(核心功能时合并manifest)，pin工程概念建议参考这篇文章[微信Android模块化架构重构实践](https://www.jianshu.com/p/3990724aa7e4)
* 多平台复用

### 一、settings.gradle中引用插件
```
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:latestversion'
    }
}
apply plugin: 'galaxybruce-pioneer-settings'
```

### 1.1 settings.gradle中从本地环境变量或者local.propertie中读取需要include的library源码路径
路径设置在本地环境变量或者local.propertie中是为了不加入版本库。
```
// LIBRARY_PATH是本地环境变量或者local.properties中配置的library路径，
// 第二个参数是打包机器上的library路径
def libraryPathWithKey = settings.ext.getLibraryPathWithKey('LIBRARY_PATH',
        '打包机器上的library路径')
```

### 1.2 settings.gradle中智能识别android module并include进来
调用以下方法可以不用手动include module，会自动识别设置的目录下的所有library
```
// 识别project根目录下的module并include进来, 参数是不需要include的目录列表
settings.ext.includeDefaultModule(['lib1', 'lib2'])
```
或者指定其他目录
```
// 识别libraryPathWithKey目录下的module并include进来，
// 参数是不需要include的目录列表
// 该方法可以多次调用或者第一个参数传多个目录
settings.ext.includeModule([libraryPathWithKey], 'lib1', 'lib2'])
```

### 1.3 读取local.properties中的值
判断local.properties中的值是否等于某个值
```
if(equalLocalValue(settings, 'FLUTTER_SOURCE', '1')) {
        setBinding(new Binding([gradle: this]))
        evaluate(new File(flutterPath + '.android/include_flutter.groovy'))
    }
```
读取local.properteis中的值
```
def value = getLocalValue(settings, 'FLUTTER_SOURCE')
```

## 二、build.gradle中引用插件
### 2.1 批量上传library到本地maven或者私有maven服务器
rootProject中的build.gradle中添加如下代码
```
apply plugin: 'galaxybruce-pioneer'

galaxybrucepioneer {
    // 这个字段可选，内部有默认的上传脚本
    mavenScriptPath = 'maven上传脚本文件路径'
    // 如果mavenScriptPath字段不填的话，需要填一下maven账号信息
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

    // 需要批量上传到maven的library配置，具体格式可参考demo中的文件
    moduleDataPath = "${project.rootDir.path}/modulemaven.json"
}

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath 'com.galaxybruce.android:pioneer-gradle-plugin:latestversion'
    }
}
```
PS: 每个library支持配置三个字段，只有name是必填字段
name: library名称
artifactId: 如果不设置，默认是project.name  
version: 如果不设置，默认是android.defaultConfig.versionName

```
{
  "group": "com.galaxybruce",
  "version": "1.0.1",
  "modules": [
    {"name": "testlibrary", "artifactId": "testlibrary", "version": ""}
  ],
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

简易脚本封装：
assembleMaven.sh
```
./gradlew uploadMaven
./gradlew uploadMaven -PplatformFlag=app1
./gradlew uploadMaven -PplatformFlag=app2
```

配置好以后，输入命令以下即可
```
$: assembleMaven
```

### 2.2 处理pin工程(核心功能时合并manifest)
插件默认开启pin工程支持，在需要的pin工程module的build.gradle中添加即可，pin工程约定都已p_开头。`建议多使用pin工程，少使用module`。
```
apply plugin: 'galaxybruce-pioneer'
```
具体使用方式参考demo。

### 2.3 多平台复用
插件默认支持多平台复用，在需要开启多平台复用的module的build.gradle中添加即可。
```
apply plugin: 'galaxybruce-pioneer'
```
同时需要在rootproject的build.gradle指定当前平台资源所在目录
```
apply plugin: 'galaxybruce-pioneer'

galaxybrucepioneer {
    platformSourceDir = 'app2'
}
```

多平台项目结构
![多平台项目结构](./images/mutilplatform.png)







