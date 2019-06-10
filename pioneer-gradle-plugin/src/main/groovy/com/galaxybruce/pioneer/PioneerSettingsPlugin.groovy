package com.galaxybruce.pioneer

import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class PioneerSettingsPlugin implements Plugin<Settings> {

    Settings settings

    void apply(Settings settings) {
        this.settings = settings
        // 通过这两种方式引用 settings.ext.getLibraryPathWithKey  或者 getLibraryPathWithKey
        // 如：def libraryPathWithKey = settings.ext.getLibraryPathWithKey('RETAIL_LIBRARY_PATH',
        //        '/Users/galaxybruce/.jenkins/workspace/retailLib/printer')
        settings.ext.getLibraryPathWithKey = Utils.&getLibraryPathWithKey
        // 也可以用这种方式
//        settings.ext {
//            getLibraryPathWithKey = Utils.&getLibraryPathWithKey
//        }
        // settings.gradle中可以调用这个方法添加module
        settings.ext.includeModule = this.&includeModule
        settings.ext.includeDefaultModule = this.&includeDefaultModule
    }

    def includeDefaultModule() {
        // 默认include根目录中的
        includeModule(settings.getRootDir().path)
    }

    def includeModule(String... libraryPaths) {
        libraryPaths.each {
            File f = new File(it) // file(it)
            if (!f.exists() || !f.isDirectory()) {
                return
            }
            if (isModule(f)) {
                settings.include "${f.name}"
                settings.project(":${f.name}").projectDir = f
                LogUtil.log(null, "PioneerSettingsPlugin", "include: " + f.name)
            } else {
                f.eachDir { dir ->
                    if (!isModule(dir)) {
                        return
                    }
                    settings.include "${dir.name}"
                    settings.project(":${dir.name}").projectDir = dir
                    LogUtil.log(null, "PioneerSettingsPlugin", "include: " + dir.name)
                }
            }
        }

    }

    static boolean isModule(File dir) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File file, String name) {
                return name == 'build.gradle' || name == 'src'
            }
        })
        return files.size() == 2
    }
}

