package com.galaxybruce.pioneer.manifest

import com.galaxybruce.pioneer.PioneerExtension
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Project

/**
 * @author bruce.zhang
 * @date 2020/11/11 17:06
 * @description
 * <p>
 * modification history:
 */
class PlatformSourceUtil {

    /**
     * 通过./gradlew uploadMaven -PplatformFlag=xxx获取，多平台打包时使用
     */
    static String gradleParamPlatformFlag

    static String getPlatformFlag(Project project) {
        String platformFlag
        if(gradleParamPlatformFlag && gradleParamPlatformFlag.trim().length() > 0) {
            platformFlag = gradleParamPlatformFlag
        } else {
            PioneerExtension extension = Utils.getPioneerExtension(project)
            if(extension.platformSourceDir) {
                platformFlag = extension.platformSourceDir
            }

            Project rootProject = project.rootProject
            if(!platformFlag) {
                platformFlag = rootProject.galaxybrucepioneer.platformSourceDir
            }

            if (!platformFlag && rootProject.hasProperty("PLATFORM_FLAG") && rootProject.PLATFORM_FLAG) {
                platformFlag = rootProject.PLATFORM_FLAG
            }
        }

        if(platformFlag == null) {
            platformFlag = ''
        }
        return platformFlag
    }
}
