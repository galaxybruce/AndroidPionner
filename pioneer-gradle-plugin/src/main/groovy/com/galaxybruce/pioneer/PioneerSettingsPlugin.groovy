package com.galaxybruce.pioneer

import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * @depcreted 已废弃，功能已移到./gradle/util.gradle文件中
 * 在settings.gradle中引入
 *
 * buildscript {
 *      apply from: './gradle/util.gradle'
 * }
 *
 */
@Deprecated
class PioneerSettingsPlugin implements Plugin<Settings> {

    Settings settings

    void apply(Settings settings) {
        this.settings = settings

        Utils.initLocalProperties(settings)

        // 通过这两种方式引用 settings.ext.getLibraryPathWithKey  或者 getLibraryPathWithKey
        // 如：def libraryPathWithKey = settings.ext.getLibraryPathWithKey('RETAIL_LIBRARY_PATH',
        //        '/Users/galaxybruce/.jenkins/workspace/retailLib/printer')
        settings.ext.getLibraryPathWithKey = Utils.&getLibraryPathWithKey
        settings.ext.getLocalValue = Utils.&getLocalValue            // 读取local.properties
        settings.ext.equalLocalValue = Utils.&equalLocalValue           // 比较local.properties中的值
        settings.ext.getParameterAnyWhere = Utils.&getParameterAnyWhere
        // 也可以用这种方式
//        settings.ext {
//            getLibraryPathWithKey = Utils.&getLibraryPathWithKey
//        }
        // settings.gradle中可以调用这个方法添加module
        settings.ext.includeModule = this.&includeModule
        settings.ext.includeDefaultModule = this.&includeDefaultModule
    }

    /**
     * 默认include根目录中的
     * @param excludeDirs
     * @return
     */
    def includeDefaultModule(List<String> excludeDirs) {
        includeModule([settings.getRootDir().path], excludeDirs)
    }

    /**
     * 加载指定目录中的module
     * 调用方式： settings.ext.includeModule([settings.getRootDir().path], ['aopstat','plugin'])
     * @param libraryPaths
     * @param excludeDirs
     * @return
     */
    def includeModule(List<String> libraryPaths, List<String> excludeDirs) {
        libraryPaths.each {
            File f = new File(it) // file(it)
            if (!f.exists() || !f.isDirectory()) {
                return
            }
            if (isModule(f)) {
                if(excludeDirs != null && excludeDirs.contains(f.name)) {
                    return
                }
                settings.include "${f.name}"
                settings.project(":${f.name}").projectDir = f
                LogUtil.log(null, "PioneerSettingsPlugin", "include: " + f.name)
            } else {
                f.eachDir { dir ->
                    if (!isModule(dir)) {
                        return
                    }
                    if(excludeDirs != null && excludeDirs.contains(dir.name)) {
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

