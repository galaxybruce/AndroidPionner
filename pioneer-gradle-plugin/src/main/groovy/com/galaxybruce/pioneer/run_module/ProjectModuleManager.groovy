package com.galaxybruce.pioneer.run_module

import com.galaxybruce.pioneer.PioneerPlugin
import com.galaxybruce.pioneer.utils.LogUtil
import org.gradle.api.Project

import java.util.regex.Pattern

/**
 * 工程中的组件module管理工具
 * 1. 用于管理组件module以application或library方式进行编译
 * 2. 用于管理组件依赖（只在给当前module进行集成打包时才添加对组件的依赖，以便于进行代码隔离）
 *
 * 注意：该功能默认关闭，如需开启，在local.properties中增加 RUN_AS_APP_FOR_COMPONENT=true
 *
 * PS：这个类中的功能来自开源项目 [https://github.com/luckybilly/CC]，之所以没有直接用CC，
 * 而是copy过来，是因为cc-register中部分功能与galaxybruce-pioneer中部分功能重叠。
 */
class ProjectModuleManager {
    static final String TAG = 'ProjectModuleManager'
    static final String PLUGIN_NAME = PioneerPlugin.PLUGIN_NAME

    // module作为app运行功能默认关闭，如需开启，在local.properties中增加 RUN_AS_APP_FOR_COMPONENT=true
    static final String RUN_AS_APP_FOR_COMPONENT = "RUN_AS_APP_FOR_COMPONENT"

    //为区别于组件单独以app方式运行的task，将组件module打包成aar时，比如上传maven仓库，
    //在local.properties文件中添加 ASSEMBLE_AAR_FOR_COMPONENT=true
    static final String ASSEMBLE_AAR_FOR_COMPONENT = "ASSEMBLE_AAR_FOR_COMPONENT"
    /**
     * 手动在gradle命令中指定当前是为哪一个module打apk
     * 主要用途：
     *      1. 插件化打包 （由于gradle命令不在正则表达式 {@link #TASK_TYPES}范围内，但需要集成打包）
     *          ./gradlew :demo_component_b:assembleDebug -PccMain=demo_component_b
     *      2. 打aar包，相反的用途，指定ccMain为一个不存在的module名称即可，可替代assemble_aar_for_component的作用
     *          ./gradlew :demo_component_b:assembleRelease -PccMain=nobody
     *          注意：此用法对于ext.mainApp=true的module无效，对于ext.alwaysLib=true的module来说无意义
     */
    static final String ASSEMBLE_APK_FOR_COMPONENT = "ccMain"
    //组件单独以app方式运行时使用的测试代码所在目录(manifest/java/assets/res等),这个目录下的文件不会打包进主app
    static final String DEBUG_DIR = "debug-test"
    //主app，一直以application方式编译
    static final String MODULE_MAIN_APP = "mainApp" 
    //apply了galaxybruce-pioneer-runmodule的module，但不是组件，而是一直作为library被其它组件依赖
    static final String MODULE_ALWAYS_LIBRARY = "alwaysLib" 

    /**
     * 以app模式运行的module name，主module除外
     */
    static String mainModuleName
    /**
     * 当前是否是打包命令
     */
    static boolean taskIsAssemble

    static boolean manageModule(Project project) {
        taskIsAssemble = false
        mainModuleName = null

        if (!project.rootProject.ext.has('runAsAppForComponent')) {
            Properties localProperties = new Properties()
            try {
                def localFile = project.rootProject.file('local.properties')
                if (localFile != null && localFile.exists()) {
                    localProperties.load(localFile.newDataInputStream())
                }
                project.rootProject.ext.runAsAppForComponent = 'true' == localProperties.getProperty(RUN_AS_APP_FOR_COMPONENT)
                project.rootProject.ext.assembleAarForComponent = 'true' == localProperties.getProperty(ASSEMBLE_AAR_FOR_COMPONENT)
            } catch (Exception ignored) {
                LogUtil.log(project, TAG, "local.properties not found")
            }
        }

        boolean runAsApp = false
        if(project.rootProject.ext.runAsAppForComponent) {
            if(new File(project.projectDir.getAbsolutePath() + "/src/${DEBUG_DIR}/AndroidManifest.xml").exists()) {
                initByTask(project)

                def mainApp = isMainApp(project)
                def assembleFor = isAssembleFor(project)
                def buildingAar = isBuildingAar(project)
                def alwaysLib = isAlwaysLib(project)

                if (mainApp) {
                    runAsApp = true
                } else if (alwaysLib || buildingAar) {
                    runAsApp = false
                } else if (assembleFor) {
                    // 这是真正的判断是否独立运行module的变量
                    runAsApp = true
                    project.ext.assembleThisModule = true
                } else if (!taskIsAssemble) {
                    // 点击"sync now"时，给apply plugin: 'com.android.application'，可以时module显示在运行列表中
                    runAsApp = true
                }
                LogUtil.log(project, TAG,
                        "mainModuleName=${mainModuleName}, project=${project.name}, runAsApp=${runAsApp} . taskIsAssemble:${taskIsAssemble}. " +
                                "settings(mainApp:${mainApp}, alwaysLib:${alwaysLib}, assembleThisModule:${assembleFor}, buildingAar:${buildingAar})")
            }
        } else {
            LogUtil.log(project, TAG, "[runAsAppForComponent] feature not open.")
        }
        project.ext.runAsApp = runAsApp
        // 这个属性给外面判断，避免重复apply application或者library
        project.ext.androidPluginApplied = true
        if (runAsApp) {
            project.apply plugin: 'com.android.application'
        } else {
            project.apply plugin: 'com.android.library'
        }

        performBuildTypeCache(project, runAsApp)
        return runAsApp
    }

    //需要集成打包相关的task
    //./gradlew :demo_component_b:clean :demo_component_b:assembleDebug
    static final String TASK_TYPES = ".*((((ASSEMBLE)|(BUILD)|(INSTALL)|((BUILD)?TINKER)|(RESGUARD)).*)|(ASR)|(ASD))"
    static void initByTask(Project project) {
        //先检查是否手动在当前gradle命令的参数中设置了mainModule的名称
        //设置方式如：
        //  ./gradlew :demo:xxxBuildPatch -PccMain=demo //用某插件化框架脚本为demo打补丁包
        //  ./gradlew :demo_component_b:assembleRelease -PccMain=anyothermodules //为demo_b打aar包
        def projectProps = project.gradle.startParameter.projectProperties
        if (projectProps && projectProps.containsKey(ASSEMBLE_APK_FOR_COMPONENT)) {
            mainModuleName = projectProps.get(ASSEMBLE_APK_FOR_COMPONENT)
            taskIsAssemble = true
            return
        }
        def taskNames = project.gradle.startParameter.taskNames
        def allModuleBuildApkPattern = Pattern.compile(TASK_TYPES)
        for (String task : taskNames) {
            if (allModuleBuildApkPattern.matcher(task.toUpperCase()).matches()) {
                taskIsAssemble = true
                if (task.contains(":")) {
                    def arr = task.split(":")
                    mainModuleName = arr[arr.length - 2].trim()
                }
                break
            }
        }
    }

    /**
     * 当前是否正在给指定的module集成打包
     */
    static boolean isAssembleFor(Project project) {
        return project.name == mainModuleName
    }

    static boolean isMainApp(Project project) {
        return project.ext.has(MODULE_MAIN_APP) && project.ext.mainApp
    }

    static boolean isAlwaysLib(Project project) {
        return project.ext.has(MODULE_ALWAYS_LIBRARY) && project.ext.alwaysLib
    }

    //判断当前设置的环境是否为组件打aar包（比如将组件打包上传maven库）
    static boolean isBuildingAar(Project project) {
        return project.rootProject.ext.assembleAarForComponent
    }

    private static void performBuildTypeCache(Project project, boolean isApp) {
//        if (!ProjectModuleCache.isSameAsLastBuildType(project, isApp)) {
//            ProjectModuleCache.cacheBuildType(project, isApp)
//            //兼容gradle3.0以上组件独立运行时出现的问题：https://github.com/luckybilly/CC/issues/62
//            //切换app/lib编译时，将transform目录清除
//            def cachedJniFile = project.file("build/intermediates/transforms/")
//            if (cachedJniFile && cachedJniFile.exists() && cachedJniFile.isDirectory()) {
//                FileUtils.deleteDirectory(cachedJniFile)
//            }
//        }
    }
}