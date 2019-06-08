package com.galaxybruce.pioneer.maven

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

    List<ModuleInfo> modules

    /**
     * 检查module是不是支持多平台
     * @param moduleName
     * @return
     */
    def getModuleInfo(String moduleName) {//  boolean supportPlatform(String moduleName)
        if(moduleMap == null && modules != null) {
            moduleMap = new HashMap<>()
            modules.forEach(new Consumer<ModuleInfo>() {
                @Override
                void accept(ModuleInfo moduleInfo) {
                    moduleMap.put(moduleInfo.name, moduleInfo)
                }
            })
        }
        ModuleInfo moduleInfo = moduleMap.get(moduleName)
        return moduleInfo
    }

    Map<String, ModuleInfo> moduleMap
}
