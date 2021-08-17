package com.galaxybruce.pioneer.utils

import com.galaxybruce.pioneer.PioneerExtension
import com.galaxybruce.pioneer.PioneerPlugin
import org.gradle.api.Project

public class Utils {

    /**
     * 也可以这样获取属性 project.galaxybrucepioneer.xxx
     * 注意：
     * 1. 使用属性之间必须先创建属性 project.extensions.create(EXT_NAME, PioneerExtension)
     * 2. 因为build.gradle中给属性赋值是在apply plugin之后，所以直接在Plugin.apply方法中获取的属性是空的
     * 3. 为了解决2中的为问题，一般是在project.afterEvaluate中获取属性，或者在subproject中可以直接使用
     *      rootProject中的属性: project.rootProject.galaxybrucepioneer.xxx
     * @param project
     * @return
     */
    public static PioneerExtension getPioneerExtension(Project project) {
        return project.extensions.findByName(PioneerPlugin.EXT_NAME) as PioneerExtension
    }

    /**
     * 也可以这样获取属性 project.rootProject.galaxybrucepioneer.xxx
     * @param project
     * @return
     */
    public static PioneerExtension getRootProjectPioneerExtension(Project project) {
        return project.rootProject.extensions.findByName(PioneerPlugin.EXT_NAME) as PioneerExtension
    }

    /**
     * 任何项目都可以使用该方法
     * @param pathKey 环境变量中的key
     * @param jenkinsPath   打包机器上的库路径
     * @return
     */
    public static String getLibraryPathWithKey(Object project, final String pathKey, final String jenkinsPath) {
        if(pathKey == null || '' == pathKey.trim()) {
            throw new IllegalStateException('pathKey must not be empty')
        }

        def libraryPath = getLocalValue(project, pathKey)
        if (!libraryPath) {
            libraryPath = System.getenv(pathKey)
            if (!libraryPath && jenkinsPath) {
                libraryPath = jenkinsPath
            }
            if(libraryPath) {
                def props = new Properties()
                def propFile = new File(project.rootDir, 'local.properties')
                props.put(pathKey, libraryPath)
                props.store(propFile.newWriter(), null)
            }
        }

        return libraryPath
    }

    /**
     * 读取properties中的值
     * @param project
     * @param key
     * @return
     */
    static String getPropertiesValue(def project, String key) {
        return project.hasProperty(key) ? project."$key" : null
        // project.hasProperty("MAVEN_MODULE_NAME") && MAVEN_MODULE_NAME ? MAVEN_MODULE_NAME : null
        // project.hasProperty("MAVEN_MODULE_NAME") && MAVEN_MODULE_NAME ? project.getProperties().get(key) : null
    }

    // 读取project.ext中的值
    static Object getExtValue(def project, String key) {
        return project.ext.has(key) ? project.ext."$key" : null
    }

    static Properties LOCAL_PROPERTIES

    /**
     * 初始化local.properties，避免后面每次都读取
     * @param project
     */
    static void initLocalProperties(Object project) {
        Properties localProperties = new Properties()
        def localPropertiesFile = new File(project.rootDir, 'local.properties')
        if (localPropertiesFile.exists()) {
            localPropertiesFile.withReader('UTF-8') { reader ->
                localProperties.load(reader)
                LOCAL_PROPERTIES = localProperties
            }
        }
        LogUtil.log(null, "PioneerPlugin", "initLocalProperties in $project : ${LOCAL_PROPERTIES}")
    }

    /**
     * 第一个参数可能是settings和project
     * @param project
     * @param key
     * @return
     */
    static String getLocalValue(Object project, String key) {
        if(key == null || '' == key.trim()) {
            throw new IllegalStateException('key must not be empty')
        }
        if(LOCAL_PROPERTIES == null) {
            initLocalProperties(project)
        }
        if(LOCAL_PROPERTIES != null) {
            return LOCAL_PROPERTIES.getProperty(key)
        }
        return null
    }

    static boolean equalLocalValue(Object project, String key, String value) {
        return value == getLocalValue(project, key)
    }


    /**
     * 获取参数，参数可能在local.proerties、gradle.properties、系统环境变量中
     * @param project
     * @param key
     * @return
     */
    static String getParameterAnyWhere(Object project, String key) {
        def value = getLocalValue(project, key)
        if(!value) {
            value = System.getenv(key)
        }
        if(!value) {
            value = System.properties[key]
        }
        if(!value) {
            value = project.getProperties().get(key)
        }
        return value
    }
}
