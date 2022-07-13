package com.galaxybruce.pioneer

import org.gradle.api.Project
/**
 * 插件属性配置信息
 * @author bruce.zhang
 * @since 17/3/28 11:48
 */
class PioneerExtension {

    // 多平台复用对应的每个平台代码所在目录，另一个作用时打成maven库时，作为库名称后缀用，不同的平台打的maven库是不一样的
    def platformSourceDir
    
    // mapping文件在git上的仓库
    def mappingRemoteUrl
    // 是否允许在打包完成后复制mapping.txt
    def copyMappingEnabled = false

    // maven上传脚本文件路径
    def mavenScriptPath
    // module配置信息
    def moduleDataPath



    /*******************maven start************************/
    def mavenUrl
    def mavenUrlSnapShot
    def mavenAccount
    def mavenPwd
    /**
     * 为true时，maven生成到指定目录：url = project.uri(project.rootProject.projectDir.absolutePath + '/repo-local')
     */
    def localMaven
    /*******************maven end  ************************/



    PioneerExtension() {}

    PioneerExtension(Project project) {}

}