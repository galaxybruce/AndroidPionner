package com.galaxybruce.pioneer

import com.android.build.gradle.AppPlugin
import com.galaxybruce.pioneer.aar.AARDependency
import com.galaxybruce.pioneer.copy.ProjectCopyOutputManager
import com.galaxybruce.pioneer.manifest.ProjectManifestMerger
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @author bruce.zhang
 * @date 2018/5/2 11:42
 * @description 理想是把各种编译上的功能汇集到该插件
 * <p>
 * modification history:
 */
public class PioneerPlugin implements Plugin<Project> {
    public static final String PLUGIN_NAME = 'galaxybruce-pioneer'
    public static final String EXT_NAME = 'galaxybrucepioneer'

    @Override
    public void apply(Project project) {
        LogUtil.log(project, "PioneerPlugin", "project(${project.name}) apply ${PLUGIN_NAME} plugin")

        // libraryPath设置
        if(project == project.rootProject) {
            handleRootProject(project)
            return
        }

        if (!project.android) {
            throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
        }
        // 判断是否是application或者libary 参考Arouter
        def isApp = project.plugins.hasPlugin(AppPlugin) // def isLibrary = project.plugins.hasPlugin(LibraryPlugin)
        project.extensions.create(EXT_NAME, PioneerExtension, project)

        if(isApp) {
            handleAppProject(project)
        }

        // 合并pin工程中的manifest
        mergeManifest(project)

        project.afterEvaluate {
            // 测试代码
//        Test.testApplicationVariants(project, isApp)
            
            project.android.defaultConfig.buildConfigField "String", "HOST_APP_NAME", "\"${project.rootProject.galaxybrucepioneer.platformSourceDir}\""
        }

    }

    private static void handleRootProject(Project project) {
        project.extensions.create(EXT_NAME, PioneerExtension)
        // 必须通过闭包的形式赋值，不然找不到对应的属性错误
        project.ext {
            // 通过这三种方式引用 settings.ext.libraryPath、rootProject.ext.libraryPath、libraryPath
//                libraryPath = Utils.getLibraryPath()
//                mavenScriptPath = libraryPath + 'buildsystem/galaxybruce_maven.gradle'

            // Utils.getLibraryPath已废弃，统一用这个方法
            getLibraryPathWithKey = Utils.&getLibraryPathWithKey
        }

        // 设置aar库相关依赖方法
        project.ext{
            isDepModule = AARDependency.&isDepModule         //是否是module依赖方式
            setAARDirs = AARDependency.&setAARDirs           //设置aar库的path
            addAARLibs = AARDependency.&addAARLibs           //添加aar依赖库
        }

        project.afterEvaluate {
            project.ext{
                platformSourceDir = project.galaxybrucepioneer.platformSourceDir
            }
        }
    }

    private static void handleAppProject(Project project) {
        // copy mapping.txt
        ProjectCopyOutputManager.copy(project)
    }

    private static void mergeManifest(Project project) {
        ProjectManifestMerger.mergeManifest(project, true)
        Task task = project.tasks['preBuild']
        task.doFirst {
            ProjectManifestMerger.mergeManifest(project, false)
        }
    }
}
