# AndroidPionner
android常用编译功能插件

## 概述
旨在把一些自动化的脚本收集在一起，比如复制mapping.txt文件、合并pin工程的manifest.xml、设置library路径、添加aar依赖。

## settings.gradle引入方式
settings.gradle文件顶部添加
```
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.galaxybruce.android:pioneer:0.0.1'
    }
}
apply plugin: 'galaxybruce-pioneer-settings'

// LIBRARY_PATH是本地环境变量中配置的library路径，第二个参数是默认的打包路径
def libraryPathWithKey = settings.ext.getLibraryPathWithKey('LIBRARY_PATH',
        '打包机器上的library路径')
```

## 项目根目录build.gradle添加
```
apply plugin: 'galaxybruce-pioneer'

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath 'com.galaxybruce.android:pioneer:0.0.1'
    }
}
```
### 一、复制mapping.txt文件
在app中的build.gradle添加
```
apply plugin: 'galaxybruce-pioneer'

// 是否copy mapping.txt
galaxybrucepioneer.copyMappingEnabled = true
```

### 二、设置library
在各个module的build.gradle最后添加
apply from: rootProject.ext.mavenScriptPath

### 三、合并pin工程manifest
在需要的pin工程module的build.gradle中添加
```
apply plugin: 'galaxybruce-pioneer'
```

### 四、设置aar依赖
调用方式：

1.现在build.gradle android节点下调用rootProject.ext.setAARDirs(project)
2.在build.gradle dependencies节点下调用rootProject.ext.addAARLibs(project, depModule











