package com.galaxybruce.pioneer


import org.gradle.api.Project

/**
 * 插件属性配置信息
 *
 * 必须在afterEvaluate中才能获取到：
 * PioneerExtension extension = Utils.getPioneerExtension(project)
 *
 * @author bruce.zhang
 * @since 17/3/28 11:48
 */
class PioneerExtension {

    // 多平台复用对应的每个平台代码所在目录，另一个作用是打成maven库时，
    // 作为库名称后缀用，为不同的平台生成不同的库
    def platformSourceDir
    
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
     * 控制maven包是否生成到本地目录
     * 为true时，maven包生成到指定目录：
     * url = project.uri(project.rootProject.projectDir.absolutePath + '/repo-local')
     */
    def localMaven
    /*******************maven end  ************************/

    // mapping文件在git上的仓库
    @Deprecated
    def mappingRemoteUrl
    // 是否允许在打包完成后复制mapping.txt
    @Deprecated
    def copyMappingEnabled = false

    PioneerExtension() {}

    PioneerExtension(Project project) {}

}