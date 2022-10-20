package com.galaxybruce.pioneer.manifest


import org.gradle.api.Project

/**
 * @author bruce.zhang
 * @date 2020/11/11 17:06
 * @description
 * <p>
 * modification history:
 */
class PlatformSourceUtil {

    static String getPlatformFlag(Project project) {
        Project rootProject = project.rootProject
        // 优先通过通过./gradlew uploadMaven -PplatformSourceDir=xxx获取参数
        String platformSourceDir = rootProject.getProperties()?.get("platformSourceDir")
        if(platformSourceDir == null || platformSourceDir.isEmpty()) {
            platformSourceDir = rootProject.galaxybrucepioneer.platformSourceDir
        }

        if(platformSourceDir == null) {
            platformSourceDir = ''
        }
        return platformSourceDir
    }
}
