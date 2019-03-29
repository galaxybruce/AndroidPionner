package com.galaxybruce.pioneer

import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class PioneerSettingsPlugin implements Plugin<Settings> {

    void apply(Settings settings) {
        // 通过这两种方式引用 settings.ext.getLibraryPathWithKey  或者 getLibraryPathWithKey
        // 如：def libraryPathWithKey = settings.ext.getLibraryPathWithKey('RETAIL_LIBRARY_PATH',
        //        '/Users/galaxybruce/.jenkins/workspace/retailLib/printer')
        settings.ext.getLibraryPathWithKey = Utils.&getLibraryPathWithKey
        // 也可以用这种方式
//        settings.ext {
//            getLibraryPathWithKey = Utils.&getLibraryPathWithKey
//        }

    }
}

