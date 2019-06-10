package com.galaxybruce.pioneer

import com.alibaba.fastjson.JSONObject
import com.android.build.gradle.AppPlugin
import com.galaxybruce.pioneer.aar.AARDependency
import com.galaxybruce.pioneer.copy.ProjectCopyOutputManager
import com.galaxybruce.pioneer.manifest.ProjectManifestMerger
import com.galaxybruce.pioneer.maven.MavenInfo
import com.galaxybruce.pioneer.maven.ModuleInfo
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import java.util.function.Consumer

/**
 * @author bruce.zhang
 * @date 2018/5/2 11:42
 * @description 理想是把各种编译上的功能汇集到该插件
 * <p>
 * modification history:
 */
class PioneerPlugin implements Plugin<Project> {
    public static final String PLUGIN_NAME = 'galaxybruce-pioneer'
    public static final String EXT_NAME = 'galaxybrucepioneer'

    @Override
    public void apply(Project project) {
        LogUtil.log(project, "PioneerPlugin", "project[${project.name}] apply ${PLUGIN_NAME} plugin")

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

    private static void handleRootProject(Project rootProject) {
        rootProject.extensions.create(EXT_NAME, PioneerExtension)
        // 必须通过闭包的形式赋值，不然找不到对应的属性错误
        rootProject.ext {
            // 通过这三种方式引用 settings.ext.libraryPath、rootProject.ext.libraryPath、libraryPath
//                libraryPath = Utils.getLibraryPath()
//                mavenScriptPath = libraryPath + 'buildsystem/galaxybruce_maven.gradle'

            // Utils.getLibraryPath已废弃，统一用这个方法
            getLibraryPathWithKey = Utils.&getLibraryPathWithKey
        }

        // 设置aar库相关依赖方法，这几个方法目前基本上没什么作用
        rootProject.ext{
            isDepModule = AARDependency.&isDepModule         //是否是module依赖方式
            setAARDirs = AARDependency.&setAARDirs           //设置aar库的path
            addAARLibs = AARDependency.&addAARLibs           //添加aar依赖库
        }

        rootProject.afterEvaluate {
            rootProject.ext{
                // 设置多平台打包时当前平台目录
                platformSourceDir = rootProject.galaxybrucepioneer.platformSourceDir
                if(!platformSourceDir && rootProject.hasProperty("MAVEN_MODULE_APP") && rootProject.MAVEN_MODULE_APP) {
                    platformSourceDir = rootProject.MAVEN_MODULE_APP
                }

                // 读取module maven配置
                if(rootProject.galaxybrucepioneer.moduleDataPath) {
                    File file = new File(rootProject.galaxybrucepioneer.moduleDataPath)
                    if(file.exists()) {
                        String fileContents = file.getText('UTF-8')
                        try {
                            mavenInfo = JSONObject.parseObject(fileContents, MavenInfo.class)
                            pomGroupId = mavenInfo.group
                        } catch (Exception e) {
                        }

                        // todo 如果mavenInfo是空，可以考虑把所有的module都上传到maven
                    }
                }
            }

            // 创建批量上传maven task
            rootProject.tasks.create(name: "uploadMaven") << {
                LogUtil.log(rootProject, "PioneerPlugin", "start upload library to maven...")

                final MavenInfo mavenInfo = project.rootProject.ext.mavenInfo
                if(mavenInfo != null && mavenInfo.modules != null && !mavenInfo.modules.isEmpty()) {
                    mavenInfo.modules.forEach(new Consumer<ModuleInfo>() {
                        @Override
                        void accept(ModuleInfo moduleInfo) {
                            def moduleName = moduleInfo.name
                            println ""
                            LogUtil.log(rootProject, "PioneerPlugin", "start upload module[$moduleName] ...")
                            def process = ("./gradlew :$moduleName:uploadArchives").execute()
                            def strErr = new StringBuffer()
                            process.consumeProcessErrorStream(strErr)
                            def result = process.waitFor()
                            if (result != 0) {
                                LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName] upload maven fail !!!!!! ")
                                println strErr.toString()
                            } else {
                                LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName] upload maven success !!! ")
                            }
                        }
                    })
                }
            }
        }

        rootProject.subprojects {
            project.afterEvaluate {
                project.plugins.withId('com.android.library') {
                    applyUpload2MavenScript(project)
                }

                project.plugins.withId('java') {
                    applyUpload2MavenScript(project)
                }
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

    private static void applyUpload2MavenScript(Project project) {
        Task uploadArchives = project.tasks.findByName("uploadArchives")
//        Task st = project.tasks.findByName("sourcesJar")
        // 如果原来的library中有上传脚本就不添加了
        if(uploadArchives.repositories.size() > 0) {
            return
        }

        final MavenInfo mavenInfo = project.rootProject.ext.mavenInfo
        final ModuleInfo moduleInfo = mavenInfo.getModuleInfo(project.name)
        if(moduleInfo != null) {
            // 给需要多平台打包的module设置平台目录，在mavenScriptPath上传maven脚本中设置
            // pom.artifactId时用到project.ext.platformSourceDir
            boolean supportPlatform = moduleInfo != null ? moduleInfo.platform : false
            if(supportPlatform){
                project.ext.platformSourceDir = project.rootProject.ext.platformSourceDir
            }

            // 在这里也可以改变pom.artifactId
            if(moduleInfo.artifactId){
                project.ext.artifactId = moduleInfo.artifactId
            }
            if(moduleInfo.ver){
                project.ext.version = moduleInfo.ver
            }

            project.apply from: project.rootProject.galaxybrucepioneer.mavenScriptPath
        }

    }
}
