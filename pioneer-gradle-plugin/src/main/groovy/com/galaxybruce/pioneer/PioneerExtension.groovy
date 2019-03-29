package com.galaxybruce.pioneer

import org.gradle.api.Project
/**
 * aop的配置信息
 * @author billy.qi
 * @since 17/3/28 11:48
 */
class PioneerExtension {

    // 是否可以显示日志
    def logEnabled = false

    // 多平台复用，每个平台代码所在目录，如特卖:temai 成长+：growplus
    def platformSourceDir

    // mapping文件copy的目录，默认是"项目同级目录/kwmapping/项目根目录名称"
    def mappingDir
    // mapping文件在git上的仓库
    def mappingRemoteUrl

    // 是否允许在打包完成后复制mapping.txt
    def copyMappingEnabled = false

    PioneerExtension(Project project) {}

}