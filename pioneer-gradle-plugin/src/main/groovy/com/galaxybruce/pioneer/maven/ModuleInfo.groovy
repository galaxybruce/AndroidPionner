package com.galaxybruce.pioneer.maven

/**
 * @author bruce.zhang
 * @date 2019-06-06 21:29
 * @description module相关信息
 * <p>
 * modification history:
 */
class ModuleInfo {

    // module name
    String name

    // 默认是module.name，如果不一致，这里需要设置
    String artifactId

    // 版本号
    String ver

    // 是否支持多平台
    boolean platform
}
