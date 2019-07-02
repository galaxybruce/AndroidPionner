package com.galaxybruce.pioneer.flutter

import com.galaxybruce.pioneer.utils.LogUtil
import org.gradle.api.Project

/**
 * @author bruce.zhang
 * @date 2019/1/16 09:41
 * @description
 *
 * <p>
 * modification history:
 */
class FlutterHandler {


    static def handleRootProject(Project rootProject) {
        def plugins = new Properties()
        def pluginsFile = new File(rootProject.rootDir.parent, '.flutter-plugins')
        if (pluginsFile.exists()) {
            pluginsFile.withReader('UTF-8') { reader -> plugins.load(reader) }
        }

        // 在project.afterEvaluate中读取group version等参数并apply上传maven脚本
        rootProject.subprojects {
            project.afterEvaluate {
                project.plugins.withId('com.android.library') {
                    if(plugins.containsKey(project.name) || 'flutter' == project.name) {
                        // 读取shell脚本中运行./gradlew xxx命令时传递的参数
                        def artGroupId = project.rootProject.hasProperty('gArtGroupId') ? project.rootProject.ext.gArtGroupId : null
                        def artVersion = project.rootProject.hasProperty('gArtVersion') ? project.rootProject.ext.gArtVersion : null

                        if(artGroupId && artVersion) {
                            project.group = artGroupId
                            project.version = artVersion
                            // 第三方的包在group里加上.thirdparty
                            if(project.projectDir.absolutePath.indexOf("pub-cache/hosted/pub.flutter-io.cn") >= 0
                                    || project.projectDir.absolutePath.indexOf("pub-cache\\hosted\\") >= 0) {
                                project.group += ".thirdparty"
                            }

                            LogUtil.log(project, "PioneerPlugin", "==group: $project.group:$project.name:$project.version")


                            // 给每个module添加上传maven脚本
//                            def mavenScriptPath = project.rootProject.file('../android_flutter_maven.gradle')
//                            project.apply from: mavenScriptPath
                            applyMaven(project)
                        }
                    }
                }
            }
        }

        // 很重要，用来覆盖各个自依赖中的group和version
        // ./gradlew clean assembleRelease -PfGroupId=${fGroupId} -PfArtifactId=${fArtifactId} -PfVersion=${fVersion}
        // 执行了这个命令，根目录的build.gradle就会执行，这时候把参数设置到project.rootProject.ext，后面各个subProject就可以拿到
        final def artGroupId = rootProject.hasProperty('fGroupId') && rootProject.fGroupId ? rootProject.fGroupId : null
        final def artVersion = rootProject.hasProperty('fVersion') && rootProject.fVersion ? rootProject.fVersion : null
        if(artGroupId && artVersion) {
            rootProject.ext {
                gArtGroupId = artGroupId
                gArtVersion = artVersion
            }
        }
    }

    static def applyMaven(Project project) {
        project.apply plugin: 'maven'

        // 这里不需要artifacts，uploadArchives命令会自动生成并上传./build/outputs/flutter-release.aar，不然出现下面错误
        // A POM cannot have multiple artifacts with the same type and classifier
        //artifacts {
        //    archives file('./build/outputs/flutter-release.aar')
        //}

        final def artGroupId = project.group
        final def artVersion = project.version
        final def artifactId = project.hasProperty('fArtifactId') && project.fArtifactId ? project.fArtifactId : null
        final def isFlutterModule = project.hasProperty('fModule') && project.fModule ? project.fModule : false


        if(artifactId == null || artVersion == null) {
            return
        }

        // 因为只要执行./gradlew xxx等命令，rootProject和subProject的build.gradle都要执行一次，
        // 所以这里要判断当前module和是否和正在处理的module一样，不是相同module就不处理，
        // 但是因为不同业务的flutter module名称都是flutter并且artifactId和module名称又不相同，所以要通过isFlutterModule参数区分
        if(!project.name.equals(artifactId) && (!project.name.equals("flutter") || !isFlutterModule)) {
            return
        }

        //project.group = artGroupId
        //project.version = artVersion

        //true: 发布到本地maven仓库， false： 发布到maven私服
//        def localMaven = project.rootProject.hasProperty("LOCAL_MAVEN") && project.rootProject.LOCAL_MAVEN.toBoolean()
        def localMaven = project.rootProject.galaxybrucepioneer.localMaven
        def mavenUrl = project.rootProject.galaxybrucepioneer.mavenUrl
        def mavenUrlSnapShot = project.rootProject.galaxybrucepioneer.mavenUrlSnapShot
        def mavenAccount = project.rootProject.galaxybrucepioneer.mavenAccount
        def mavenPwd = project.rootProject.galaxybrucepioneer.mavenPwd

        LogUtil.log(project, "PioneerPlugin", "${localMaven} : ${mavenUrl} : ${mavenUrlSnapShot} : ${mavenAccount} : ${mavenPwd}")

        project.uploadArchives {
            repositories {
                mavenDeployer {
                    LogUtil.log(project, "PioneerPlugin", "==maven url: ${artGroupId}:${artifactId}:${artVersion}")

                    if(localMaven && localMaven == true) {
                        repository(url: uri(project.rootProject.projectDir.absolutePath + '/repo-local'))
                    } else {
                        repository(url: mavenUrl) {
                            authentication(userName: mavenAccount, password: mavenPwd)
                        }

                        snapshotRepository(url: mavenUrlSnapShot) {
                            authentication(userName: mavenAccount, password: mavenPwd)
                        }
                    }

                    pom.groupId = artGroupId
                    pom.artifactId = artifactId
                    pom.version = artVersion

                    pom.project {
                        licenses {
                            license {
                                name 'The Apache Software License, artVersion 2.0'
                                url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                            }
                        }
                    }
                }
            }
        }
    }

}
