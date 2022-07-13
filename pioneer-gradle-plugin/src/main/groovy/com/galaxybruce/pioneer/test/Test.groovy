package com.galaxybruce.pioneer.test

import com.galaxybruce.pioneer.PioneerExtension
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * 调用UI界面工具
 * Android/sdk/tools/proguard/bin/proguardgui.sh
 * 采用命令
 * retrace.bat -verbose mapping.txt stacktrace.txt > out.txt
 */
public class Test {

    /**
     * all和each的区别：https://stackoverflow.com/questions/31802752/what-is-the-difference-between-all-and-each-in-gradle
     * 安卓插件里的变量最好都用all
     * @param project
     * @param isApp
     */
    public static void testAllAndEach(Project project, boolean isApp) {
        if(isApp) {
            project.android.productFlavors.all { flavor ->
                LogUtil.log(project,"Test.flavor", flavor.name)
            }

            project.android.productFlavors.each { flavor ->
                LogUtil.log(project,"Test.flavor", flavor.name)
            }

            // 只有放在project.afterEvaluate之后才会有
            project.android.applicationVariants.each { variant ->
                LogUtil.log(project,"Test.variant.each", variant.toString())
            }

            // 放在project.afterEvaluate之前和之后都会有
            project.android.applicationVariants.all { variant ->
                LogUtil.log(project,"Test.variant.all", variant.toString())

                variant.outputs.all { output ->
                    LogUtil.log(project,"Test.variant.all.outputFileName", output.outputFileName)
                }
                variant.outputs.each { output ->
                    LogUtil.log(project,"Test.variant.each.outputFileName", output.outputFileName)
                }
            }
        }
    }

    public static void testApplicationVariants(Project project, boolean isApp) {
        if(isApp) {
            // 放在project.afterEvaluate之前和之后都会有
            project.android.applicationVariants.all { variant ->
                // debug或者release
                LogUtil.log(project,"Test.variant.name", variant.name)
                LogUtil.log(project,"Test.variant.name.capitalize", variant.name.capitalize())

                // 首字母大写
                String variantName = variant.name.capitalize()
                Task preBuild = project.tasks["pre${variantName}Build"] // project.tasks.findByName("pre${variantName}Build")

                if (preBuild) {
                    LogUtil.log(project,"Test.preBuild", "pre${variantName}Build")
                }

            }
        }
    }



}
