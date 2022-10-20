package com.galaxybruce.pioneer.maven

import com.galaxybruce.pioneer.utils.LogUtil

import java.util.function.Consumer

/**
 * @author bruce.zhang
 * @date 2019-06-06 21:29
 * @description maven相关信息
 * <p>
 * modification history:
 */
class MavenInfo {

    String group
    String version

    List<ModuleInfo> modules

    /**
     * 需要多平台打包的modules, key是对应的平台目录名称
     */
    Map<String, List<ModuleInfo>> platform_modules

    /**
     * 初始化后所有module的缓存，key是project.name
     */
    Map<String, ModuleInfo> moduleMap = new HashMap<>()

    /**
     * 检查module是不是支持多平台
     * @param moduleName
     * @return
     */
    def getModuleInfo(String moduleName) {
        ModuleInfo moduleInfo = moduleMap?.get(moduleName)
        return moduleInfo
    }

    void initModuleInfo(String platformSourceDir) {
        if(modules != null) {
            modules.forEach(new Consumer<ModuleInfo>() {
                @Override
                void accept(ModuleInfo moduleInfo) {
                    moduleMap.put(moduleInfo.name, moduleInfo)
                }
            })
        }

        if(platform_modules != null && platform_modules.size() > 0) {
            if(platformSourceDir == null || platformSourceDir.isEmpty()) {
                LogUtil.log(null, "MavenUploadManager",
                        "app platform flag not config.")
            } else {
                LogUtil.log(null, "MavenUploadManager",
                        "app platform flag: ${platformSourceDir}")
            }

            List<ModuleInfo> platformModules = platform_modules.get(platformSourceDir)
            if(platformModules != null) {
                platformModules.forEach(new Consumer<ModuleInfo>() {
                    @Override
                    void accept(ModuleInfo moduleInfo) {
                        moduleInfo.platform = true
                        moduleMap.put(moduleInfo.name, moduleInfo)
                    }
                })
            }
        }
    }

    List<ModuleInfo> getModules(String platformSourceDir) {
        if(platformSourceDir != null && platformSourceDir.trim().length() > 0) {
            return platform_modules.get(platformSourceDir)
        } else {
            return modules
        }
    }

}
