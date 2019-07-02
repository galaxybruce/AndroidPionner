package com.galaxybruce.pioneer

import org.gradle.api.Project
/**
 * 属性配置信息
 * @author bruce.zhang
 * @since 17/3/28 11:48
 */
class PioneerExtension {

    // 是否可以显示日志
    def logEnabled = true

    // 多平台复用对应的每个平台代码所在目录，另一个作用时打成maven库时，作为库名称后缀用，不同的平台打的maven库是不一样的
    def platformSourceDir

    // mapping文件copy的目录，默认是"项目同级目录/mapping/项目根目录名称"
    def mappingDir
    // mapping文件在git上的仓库
    def mappingRemoteUrl

    // 是否允许在打包完成后复制mapping.txt
    def copyMappingEnabled = false

    // maven上传脚本文件路径
    def mavenScriptPath
    // module配置信息
    def moduleDataPath



    /*******************flutter start************************/
    def mavenUrl
    def mavenUrlSnapShot
    def mavenAccount
    def mavenPwd
    def localMaven
    /*******************flutter end  ************************/



    PioneerExtension() {}

    PioneerExtension(Project project) {}

}