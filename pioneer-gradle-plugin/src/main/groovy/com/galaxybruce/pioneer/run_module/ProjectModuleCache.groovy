package com.galaxybruce.pioneer.run_module

import com.android.builder.model.AndroidProject
import com.galaxybruce.pioneer.PioneerPlugin
import org.gradle.api.Project

/**
 * 文件操作辅助类
 *
 */
class ProjectModuleCache {
    final static def CACHE_INFO_DIR = PioneerPlugin.PLUGIN_NAME

    private static File getCacheFile(Project project, String fileName) {
        String baseDir = getCacheFileDir(project)
        if (mkdirs(baseDir)) {
            return new File(baseDir + fileName)
        } else {
            throw new FileNotFoundException("Not found  path:" + baseDir)
        }
    }

    static boolean isSameAsLastBuildType(Project project, boolean isApp) {
        File cacheFile = getCacheFile(project, "build-type.json")
        if (cacheFile.exists()) {
            return (cacheFile.text == 'true') == isApp
        }
        return false
    }

    static void cacheBuildType(Project project, boolean isApp) {
        File cacheFile = getCacheFile(project, "build-type.json")
        cacheFile.getParentFile().mkdirs()
        if (!cacheFile.exists())
            cacheFile.createNewFile()
        cacheFile.write(isApp.toString())
    }

    private static String getCacheFileDir(Project project) {
        return project.getBuildDir().absolutePath + File.separator +
                AndroidProject.FD_INTERMEDIATES + File.separator +
                CACHE_INFO_DIR + File.separator
    }

    /**
     * 创建文件夹
     * @param dirPath
     * @return boolean
     */
    static boolean mkdirs(String dirPath) {
        def baseDirFile = new File(dirPath)
        def isSuccess = true
        if (!baseDirFile.isDirectory()) {
            isSuccess = baseDirFile.mkdirs()
        }
        return isSuccess
    }

}