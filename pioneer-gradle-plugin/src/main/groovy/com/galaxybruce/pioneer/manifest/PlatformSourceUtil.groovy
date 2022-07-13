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
        // 通过./gradlew uploadMaven -PplatformFlag=xxx获取，多平台打包时使用
        String platformFlag = rootProject.getProperties()?.get("platformFlag")
        if(platformFlag == null || platformFlag.isEmpty()) {
            platformFlag = rootProject.galaxybrucepioneer.platformSourceDir
        }

        if(platformFlag == null) {
            platformFlag = ''
        }
        return platformFlag
    }
}
