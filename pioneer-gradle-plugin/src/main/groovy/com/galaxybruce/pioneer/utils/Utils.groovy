package com.galaxybruce.pioneer.utils

import com.galaxybruce.pioneer.PioneerExtension
import com.galaxybruce.pioneer.PioneerPlugin
import org.gradle.api.Project

public class Utils {

    public static PioneerExtension getPioneerExtension(Project project) {
        return project.extensions.findByName(PioneerPlugin.EXT_NAME) as PioneerExtension
    }

    /**
     * 任何项目都可以使用该方法
     * @param pathKey 环境变量中的key
     * @param jenkinsPath   打包机器上的库路径
     * @return
     */
    public static String getLibraryPathWithKey(final String pathKey, final String jenkinsPath) {
        if(pathKey == null || ''.equals(pathKey.trim())) {
            throw new IllegalStateException('pathKey must not be empty')
        }

        def props = new Properties()
        def propFile = new File("local.properties")
        propFile.withInputStream {
            stream -> props.load(stream)
        }

        def libraryPath = props.getProperty(pathKey)
        if (!libraryPath) {
            libraryPath = System.getenv(pathKey)
            if (!libraryPath && jenkinsPath) {
                libraryPath = jenkinsPath
            }
            if(libraryPath) {
                props.put(pathKey, libraryPath)
                props.store(propFile.newWriter(), null)
            }
        }

        return libraryPath
    }
}
