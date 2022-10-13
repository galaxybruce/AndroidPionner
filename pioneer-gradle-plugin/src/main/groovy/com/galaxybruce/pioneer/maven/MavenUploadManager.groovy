package com.galaxybruce.pioneer.maven

import com.android.Version
import com.galaxybruce.pioneer.manifest.PlatformSourceUtil
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import com.galaxybruce.pioneer.utils.runtime.ExecuteResult
import com.galaxybruce.pioneer.utils.runtime.RunTimeTask
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GradleVersion

import java.util.function.Consumer

/**
 * @author bruce.zhang
 * @date 2020/11/11 11:34
 * @description android module 批量上传到maven仓库
 * <p>
 * modification history:
 */
class MavenUploadManager {

    /**
     * library module设置上传maven task
     * @param rootProject
     */
    static void setModuleUploadMaven(Project rootProject) {
        rootProject.afterEvaluate {
            // 创建批量上传maven task
            rootProject.tasks.create(name: "uploadMaven") {
                doLast {
                    LogUtil.log(rootProject, "PioneerPlugin", "start upload library to maven...")

                    final MavenInfo mavenInfo = Utils.getExtValue(rootProject, "mavenInfo")
                    final List<ModuleInfo> modules = mavenInfo?.getModules(PlatformSourceUtil.getPlatformFlag(rootProject))
                    if (modules != null && !modules.isEmpty()) {
                        modules.forEach(new Consumer<ModuleInfo>() {
                            @Override
                            void accept(ModuleInfo moduleInfo) {
                                def moduleName = moduleInfo.name
                                println ""

                                Project subProject = rootProject.findProject(moduleName)
                                if(subProject == null) {
                                    LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName] not found, please check json config file !!! ")
                                    return
                                }

                                LogUtil.log(rootProject, "PioneerPlugin", "start upload module[$moduleName] ...")
                                // ./gradlew :module1:publish :module2:publish :module3:publish
                                // rootProject.project(":$moduleName").tasks['publish'].execute()

                                // command命令是在另一个进程中，需要把参数透传过去
                                final String param = "-PplatformFlag=" + PlatformSourceUtil.getPlatformFlag(subProject)
                                if (OperatingSystem.current().isWindows()) {
                                    // window下用process.waitFor会出现死锁
                                    def eo = new ByteArrayOutputStream()
                                    def so = new ByteArrayOutputStream()
                                    def result = rootProject.exec {
                                        commandLine 'gradlew.bat', ":$moduleName:clean", ":$moduleName:publish", "${param}"
                                        standardOutput so
                                        errorOutput eo
                                        // Gradle will by default throw an exception and terminate when receiving non-zero result codes from commands
                                        ignoreExitValue true
                                    }

                                    def pomArtifactId = moduleName
                                    if (subProject.ext.has('platformFlag') && subProject.ext.platformFlag) {
                                        pomArtifactId += '-' + subProject.ext.platformFlag
                                    }
                                    // https://stackoverflow.com/questions/45396268/gradle-exec-how-to-print-error-output
                                    if (result.exitValue != 0) {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[${subProject.group}:$pomArtifactId:${subProject.version}] upload maven fail !!!!!! ")
                                        println eo.toString()
                                    } else {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[${subProject.group}:$pomArtifactId:${subProject.version}] upload maven success !!! ")
                                    }
                                } else {
//                                    def cmd = "./gradlew"
//                                    def process = ("$cmd :$moduleName:publish").execute()
//                                    def strErr = new StringBuffer()
//                                    process.consumeProcessErrorStream(strErr)
//                                    def result = process.waitFor() // 这里会出现死锁

                                    String command = String.format("./gradlew :%s:clean :%s:publish %s", moduleName, moduleName, param)
                                    ExecuteResult executeResult = RunTimeTask.executeCommand(command, Integer.MAX_VALUE)

                                    def pomArtifactId = moduleName
                                    if (subProject.ext.has('platformFlag') && subProject.ext.platformFlag) {
                                        pomArtifactId += '-' + subProject.ext.platformFlag
                                    }
                                    if (executeResult.exitCode != 0) {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[${subProject.group}:$pomArtifactId:${subProject.version}] upload maven fail !!!!!! ")
                                        println executeResult.toString()
                                    } else {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[${subProject.group}:$pomArtifactId:${subProject.version}] upload maven success !!! ")
                                    }
                                }
                            }
                        })
                    } else {
                        LogUtil.log(rootProject, "PioneerPlugin", "no corresponding modules founded, please check json config file !!! ")
                    }
                }
            }
        }

        // 为每个project添加publish任务
        rootProject.subprojects {
            project.afterEvaluate {
                project.plugins.withId('com.android.library') {
                    configProjectInfo(project)
                }

                project.plugins.withId('java') {
                    configProjectInfo(project)
                }
            }
        }
    }

    private static void configProjectInfo(Project project) {
        // todo 这里是否要排除flutter中依赖的插件module

        // project.group和version先设置默认的
        final MavenInfo mavenInfo = Utils.getExtValue(project.rootProject, "mavenInfo")
        final ModuleInfo moduleInfo = mavenInfo?.getModuleInfo(project.name)
        if(moduleInfo != null) {
            if(mavenInfo.group) {
                project.group = mavenInfo.group
            }
            if(moduleInfo.version) {
                project.version = moduleInfo.version
            } else if(mavenInfo.version){
                project.version = mavenInfo.version
            }
            // 在这里也可以改变pom.artifactId
            if(moduleInfo.artifactId){
                project.ext.artifactId = moduleInfo.artifactId
            }

            // 给需要多平台打包的module设置平台目录，在mavenScriptPath上传maven脚本中设置
            // pom.artifactId时用到project.ext.platformFlag
            boolean supportPlatform = moduleInfo != null ? moduleInfo.platform : false
            if(supportPlatform){
                project.ext.platformFlag = PlatformSourceUtil.getPlatformFlag(project)
            }

            // apply maven plugin
            applyUploadMavenScript(project)
        } else {
            // todo 其他没在json配置文件中配置过的project是否要设置group和version
        }
    }

    private static void applyUploadMavenScript(Project project) {
        if(!project.rootProject.galaxybrucepioneer.mavenScriptPath) {
            applyUploadDefaultMavenScript(project)
        } else {
            project.apply from: project.rootProject.galaxybrucepioneer.mavenScriptPath
        }
    }

    private static void applyUploadDefaultMavenScript(Project project) {
        // 如果原来的library中有上传脚本就不添加了
        var publishingExt = project.extensions.findByName('publishing') // project.tasks.findByName("publish").repositories
        if(publishingExt != null && publishingExt.repositories.size() > 0) {
            return
        }

        // 参考 https://github.com/alibaba/ARouter/blob/develop/gradle/publish.gradle
        // 注意：app中不能设置多渠道，否则上传maven中没有aar!!!
        project.apply plugin: 'maven-publish'
        def isAndroid = project.hasProperty("android") ||
                project.getPlugins().hasPlugin('com.android.application') ||
                project.getPlugins().hasPlugin('com.android.library')
        if (isAndroid) { // Android libraries
            if(GradleVersion.version(Version.ANDROID_GRADLE_PLUGIN_VERSION) >= GradleVersion.version('7.1.2')) {
                android {
                    publishing {
                        singleVariant('release') {
                            withSourcesJar()
                            withJavadocJar()
                        }
                    }
                }
            }

//            project.task('androidJavadocs', type: Javadoc) {
//                source = project.android.sourceSets.main.java.srcDirs
//                classpath += project.files(project.android.getBootClasspath().join(File.pathSeparator))
//            }
//
//            project.task('androidJavadocsJar', type: Jar, dependsOn: project.androidJavadocs) {
//                getArchiveClassifier().set('javadoc')
//                from project.androidJavadocs.destinationDir
//            }

            project.task('androidSourcesJar', type: Jar) {
                getArchiveClassifier().set('sources')
                from project.android.sourceSets.main.java.srcDirs
            }

            project.artifacts {
                archives project.tasks.androidSourcesJar
//                archives project.tasks.androidJavadocsJar
            }
        } else { // Java libraries
//            project.task('javadocJar', type: Jar, dependsOn: project.javadoc) {
//                getArchiveClassifier().set('javadoc')
//                from project.javadoc.destinationDir
//            }

            project.task('sourceJar', type: Jar, dependsOn: project.classes) {
                getArchiveClassifier().set('sources')
                from project.android.sourceSets.main.allSource
            }

            project.artifacts {
                archives project.tasks.sourceJar
//                archives project.tasks.javadocJar
            }
        }

        Properties properties = new Properties()
        File rootProjectPropertiesFile = project.rootProject.file("gradle.properties")
        if (rootProjectPropertiesFile.exists()) {
            properties.load(rootProjectPropertiesFile.newDataInputStream())
        }

        def localMaven = project.rootProject.galaxybrucepioneer.localMaven
        def mavenUrl = project.rootProject.galaxybrucepioneer.mavenUrl
        def mavenUrlSnapShot = project.rootProject.galaxybrucepioneer.mavenUrlSnapShot
        def mavenAccount = project.rootProject.galaxybrucepioneer.mavenAccount
        def mavenPwd = project.rootProject.galaxybrucepioneer.mavenPwd
        if (!mavenUrl) {
            // read properties
            mavenUrl = properties.getProperty("MAVEN_URL")
            mavenUrlSnapShot = properties.getProperty("MAVEN_URL_SNAPSHOT")
            mavenAccount = properties.getProperty("MAVEN_ACCOUNT_NAME")
            mavenPwd = properties.getProperty("MAVEN_ACCOUNT_PWD")
        }

        def pomGroupId = project.group
        def pomArtifactId = project.name
        if(project.version == Project.DEFAULT_VERSION) {
            project.version='1.0.0'
        }
        def pomVersion = project.version

        if (project.ext.has('artifactId') && project.ext.artifactId) {
            pomArtifactId = project.ext.artifactId
        }
        if (project.ext.has('platformFlag') && project.ext.platformFlag) {
            pomArtifactId += '-' + project.ext.platformFlag
        }

        LogUtil.log(project, "PioneerPlugin",
                "======maven configuration project: ${project.name} -- mavenAddress: ${pomGroupId}:${pomArtifactId}:${pomVersion}")

        project.afterEvaluate {
            publishingExt = (PublishingExtension) project.extensions.findByName('publishing')
            publishingExt.publications() {
                mavenProduction(MavenPublication) {
                    //group,artifactId和version
                    groupId = pomGroupId
                    artifactId = pomArtifactId
                    version = pomVersion

                    if (isAndroid) {
                        project.afterEvaluate {
                            from project.components.release
                        }
                        artifact project.tasks.androidSourcesJar
//                        artifact project.tasks.androidJavadocsJar
                    } else {
                        project.afterEvaluate {
                            from project.components.java
                        }
                        artifact project.tasks.sourceJar
//                        artifact project.tasks.javadocJar
                    }

                    pom {
                        licenses {
                            license {
                                name = 'The Apache License, Version 2.0'
                                url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                            }
                        }

                        withXml {
                            asNode().dependencies.'*'.findAll() {
                                it.scope.text() == 'runtime' && project.configurations.implementation.allDependencies.find { dep ->
                                    dep.name == it.artifactId.text()
                                }
                            }.each {
                                it.scope*.value = 'compile'
                            }
                        }
                    }
                }
            }
            publishingExt.repositories() {
                maven {
                    if(localMaven == true) {
                        url = project.uri(project.rootProject.projectDir.absolutePath + '/repo-local')
                    } else {
                        url = pomVersion.endsWith('SNAPSHOT') ? mavenUrlSnapShot : mavenUrl
                        credentials {
                            username = mavenAccount
                            password = mavenPwd
                        }
                        allowInsecureProtocol = true
                    }
                }
            }
        }
    }
}
