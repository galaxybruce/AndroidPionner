package com.galaxybruce.pioneer

import com.alibaba.fastjson.JSONObject
import com.android.build.gradle.AppPlugin
import com.galaxybruce.pioneer.aar.AARDependency
import com.galaxybruce.pioneer.manifest.PlatformSourceUtil
import com.galaxybruce.pioneer.manifest.ProjectManifestMerger
import com.galaxybruce.pioneer.maven.MavenInfo
import com.galaxybruce.pioneer.maven.MavenUploadManager
import com.galaxybruce.pioneer.run_module.ProjectModuleManager
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @author bruce.zhang
 * @date 2018/5/2 11:42
 * @description
 * <p>
 * modification history:
 */
class PioneerPlugin implements Plugin<Project> {
    public static final String PLUGIN_NAME = 'galaxybruce-pioneer'
    public static final String EXT_NAME = 'galaxybrucepioneer'

    @Override
    public void apply(Project project) {
        boolean isRootProject = project == project.rootProject
        LogUtil.log(project, "PioneerPlugin",
                "${isRootProject ? "root " : ""}project[${project.name}] apply `${PLUGIN_NAME}` plugin")

        if(isRootProject) {
            handleRootProject(project)
        } else {
            handleSubProject(project)
        }
    }

    private static void handleRootProject(Project rootProject) {
        Utils.initLocalProperties(rootProject)
        rootProject.extensions.create(EXT_NAME, PioneerExtension)
        setRootProjectExtValues(rootProject)
        MavenUploadManager.setModuleUploadMaven(rootProject)
//        FlutterHandler.handleRootProject(rootProject)
    }

    private static void setRootProjectExtValues(Project rootProject) {
        // 必须通过闭包的形式赋值，不然找不到对应的属性错误
        rootProject.ext {
            // 通过这三种方式引用 settings.ext.libraryPath、rootProject.ext.libraryPath、libraryPath
//                libraryPath = Utils.getLibraryPath()
//                mavenScriptPath = libraryPath + 'buildsystem/galaxybruce_maven.gradle'

            // Utils.getLibraryPath已废弃，统一用这个方法
            getLibraryPathWithKey = Utils.&getLibraryPathWithKey
            getLocalValue = Utils.&getLocalValue            // 读取local.properties
            equalLocalValue = Utils.&equalLocalValue           // 比较local.properties中的值
            getParameterAnyWhere = Utils.&getParameterAnyWhere            // 读取参数
        }

        // 设置aar库相关依赖方法，这几个方法目前基本上没什么作用
        rootProject.ext {
            setAARDirs = AARDependency.&setAARDirs           //设置aar库的path
            addAARLibs = AARDependency.&addAARLibs           //添加aar依赖库
        }

        rootProject.afterEvaluate {
            rootProject.ext {
                // 读取module maven配置
                if (rootProject.galaxybrucepioneer.moduleDataPath) {
                    File file = new File(rootProject.galaxybrucepioneer.moduleDataPath)
                    if (file.exists()) {
                        String fileContents = file.getText('UTF-8')
                        try {
                            mavenInfo = JSONObject.parseObject(fileContents, MavenInfo.class)
                            mavenInfo?.initModuleInfo(PlatformSourceUtil.getPlatformFlag(rootProject))
                        } catch (Exception e) {
                            e.printStackTrace()
                        }

                        // todo 如果mavenInfo是空，可以考虑把所有的module都上传到maven
                    }
                }
            }
        }
    }

    private static void handleSubProject(Project project) {
//        if (!project.android) {
//            throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
//        }

        def isApp = project.plugins.hasPlugin(AppPlugin) // def isLibrary = project.plugins.hasPlugin(LibraryPlugin)
        if(isApp) {
            handleAppProject(project)
        } else {
            // 用于管理组件module以application或library方式进行编译
            handleModuleProject(project)
        }
        // 合并pin工程中的manifest
        mergeManifest(project)
        // 给BuildConfig.java中设置字段
        setBuildConfigField(project)
    }

    private static void handleAppProject(Project project) {
        // copy mapping.txt
//        ProjectCopyOutputManager.copy(project)
    }

    private static void handleModuleProject(Project project) {
        ProjectModuleManager.manageModule(project)
    }

    private static void mergeManifest(Project project) {
        ProjectManifestMerger.mergeManifest(project)
        Task task = project.tasks['preBuild']
        task.doFirst {
            // 这里重新执行一遍，是因为如果命令中带有clean命令，会清除掉之前生成的manifest
            def taskNames = project.gradle.startParameter.taskNames
            def isCleanTask = taskNames.toString().toLowerCase().contains("clean")
            ProjectManifestMerger.mergeManifest(project, isCleanTask)
        }
    }

    private static void setBuildConfigField(Project project) {
        project.afterEvaluate {
            // 测试代码
//            Test.testApplicationVariants(project, isApp)
            project.android.defaultConfig.buildConfigField "String", "APP_PLATFORM_FLAG", "\"${PlatformSourceUtil.getPlatformFlag(project)}\""
        }
    }
}
