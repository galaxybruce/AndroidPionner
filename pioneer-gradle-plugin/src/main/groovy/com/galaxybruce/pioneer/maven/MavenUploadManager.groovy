package com.galaxybruce.pioneer.maven

import com.galaxybruce.pioneer.manifest.PlatformSourceUtil
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import com.galaxybruce.pioneer.utils.runtime.ExecuteResult
import com.galaxybruce.pioneer.utils.runtime.RunTimeTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.internal.os.OperatingSystem

import java.util.function.Consumer;

/**
 * @author bruce.zhang
 * @date 2020/11/11 11:34
 * @description (亲 ， 我是做什么的)
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

                    final MavenInfo mavenInfo = Utils.getExtValue(project.rootProject, "mavenInfo")
                    final List<ModuleInfo> modules = mavenInfo?.getModules(PlatformSourceUtil.gradleParamPlatformFlag)
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
                                // ./gradlew :module1:uploadArchives :module2:uploadArchives :module3:uploadArchives
                                // rootProject.project(":$moduleName").tasks['uploadArchives'].execute()

                                // command命令是在另一个进程中，需要把参数透传过去
                                final String param = PlatformSourceUtil.isGradleParamPlatformFlagValid() ?
                                        "-PplatformFlag=" + PlatformSourceUtil.gradleParamPlatformFlag : ""
                                if (OperatingSystem.current().isWindows()) {
                                    // window下用process.waitFor会出现死锁
                                    def eo = new ByteArrayOutputStream()
                                    def so = new ByteArrayOutputStream()
                                    def result = rootProject.exec {
                                        commandLine 'gradlew.bat', ":$moduleName:clean", ":$moduleName:uploadArchives", "${param}"
                                        standardOutput so
                                        errorOutput eo
                                        // Gradle will by default throw an exception and terminate when receiving non-zero result codes from commands
                                        ignoreExitValue true
                                    }
                                    // https://stackoverflow.com/questions/45396268/gradle-exec-how-to-print-error-output
                                    if (result.exitValue != 0) {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName-${subProject.version}] upload maven fail !!!!!! ")
                                        println eo.toString()
                                    } else {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName-${subProject.version}] upload maven success !!! ")
                                    }
                                } else {
//                                    def cmd = "./gradlew"
//                                    def process = ("$cmd :$moduleName:uploadArchives").execute()
//                                    def strErr = new StringBuffer()
//                                    process.consumeProcessErrorStream(strErr)
//                                    def result = process.waitFor() // 这里会出现死锁

                                    String command = String.format("./gradlew :%s:clean :%s:uploadArchives %s", moduleName, moduleName, param)
                                    ExecuteResult executeResult = RunTimeTask.executeCommand(command, Integer.MAX_VALUE)
                                    if (executeResult.exitCode != 0) {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName-${subProject.version}] upload maven fail !!!!!! ")
                                        println executeResult.toString()
                                    } else {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName-${subProject.version}] upload maven success !!! ")
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

        // 为每个project添加uploadArchives任务
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

    private static void applyUpload2MavenScript(Project project) {
        // 如果原来的library中有上传脚本就不添加了
        var repositories = project.extensions.getByName('publishing').repositories // project.tasks.findByName("uploadArchives").repositories
        if(repositories.size() > 0) {
            return
        }

        // todo 这里是否要排除flutter中依赖的插件module

        // project.group和version先设置默认的
        final MavenInfo mavenInfo = Utils.getExtValue(project.rootProject, "mavenInfo")
        final ModuleInfo moduleInfo = mavenInfo?.getModuleInfo(project.name)
        if(moduleInfo != null) {
            project.group = mavenInfo.group
            if(mavenInfo.version) {
                project.version = mavenInfo.version
            }

            // 给需要多平台打包的module设置平台目录，在mavenScriptPath上传maven脚本中设置
            // pom.artifactId时用到project.ext.platformFlag
            boolean supportPlatform = moduleInfo != null ? moduleInfo.platform : false
            if(supportPlatform){
                project.ext.platformFlag = PlatformSourceUtil.getPlatformFlag(project)
            }

            // 在这里也可以改变pom.artifactId
            if(moduleInfo.artifactId){
                project.ext.artifactId = moduleInfo.artifactId
            }
            project.ext.version = mavenInfo.version
            if(moduleInfo.version){
                project.ext.version = moduleInfo.version
            }


            if(!project.rootProject.galaxybrucepioneer.mavenScriptPath) {
                applyUploadDefaultMavenScript(project)
            } else {
                project.apply from: project.rootProject.galaxybrucepioneer.mavenScriptPath
            }
        } else {
            // todo 其他没在json配置文件中配置过的project是否要设置group和version
        }
    }

    private static void applyUploadDefaultMavenScript(Project project) {
        // 参考./gradle/upload_maven.gradle
        // 注意：app中不能设置多渠道，否则上传maven中没有aar!!!
        project.apply plugin: 'maven-publish'

        if (project.hasProperty("android") ||
                project.getPlugins().hasPlugin('com.android.application') ||
                project.getPlugins().hasPlugin('com.android.library')) { // Android libraries
//            task javadocs(type: Javadoc) {
//                source = android.sourceSets.main.java.srcDirs
//                classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
//            }

            project.task('javadocJar', type: Jar, dependsOn: project.javadoc) {
                getArchiveClassifier().set('javadoc')
                from project.javadoc.destinationDir
            }

            project.task('sourceJar', type: Jar) {
                getArchiveClassifier().set('sources')
                from project.android.sourceSets.main.java.srcDirs
            }

            project.artifacts {
                archives project.tasks.sourceJar
                archives project.tasks.javadocJar
            }
        } else { // Java libraries
            project.task('javadocJar', type: Jar, dependsOn: project.javadoc) {
                getArchiveClassifier().set('javadoc')
                from project.javadoc.destinationDir
            }

            project.task('sourceJar', type: Jar, dependsOn: project.classes) {
                getArchiveClassifier().set('sources')
                from project.android.sourceSets.main.allSource
            }

            project.artifacts {
                archives project.tasks.sourceJar
                archives project.tasks.javadocJar
            }
        }

        Properties properties = new Properties()
        File rootProjectPropertiesFile = project.rootProject.file("gradle.properties")
        if (rootProjectPropertiesFile.exists()) {
            properties.load(rootProjectPropertiesFile.newDataInputStream())
        }

        def mavenUrl = project.rootProject.galaxybrucepioneer.mavenUrl
        def mavenUrlSnapShot = project.rootProject.galaxybrucepioneer.mavenUrlSnapShot
        def mavenAccount = project.rootProject.galaxybrucepioneer.mavenAccount
        def mavenPwd = project.rootProject.galaxybrucepioneer.mavenPwd
        if(!mavenUrl) {
            // read properties
//            localMaven = properties.getProperty("LOCAL_MAVEN")
            mavenUrl = properties.getProperty("MAVEN_URL")
            mavenUrlSnapShot = properties.getProperty("MAVEN_URL_SNAPSHOT")
            mavenAccount = properties.getProperty("MAVEN_ACCOUNT_NAME")
            mavenPwd = properties.getProperty("MAVEN_ACCOUNT_PWD")
        }

        def pomGroupId = properties.getProperty("project.groupId")
        def pomArtifactId = project.name
        def pomVersion = project.version

        if(project.rootProject.ext.has('pomGroupId') && project.rootProject.ext.pomGroupId) {
            pomGroupId = project.rootProject.ext.pomGroupId
        }

        if(project.ext.has('artifactId') && project.ext.artifactId) {
            pomArtifactId = project.ext.artifactId
        }

        if(project.ext.has('platformFlag') && project.ext.platformFlag) {
            pomArtifactId += '-' + project.ext.platformFlag
        }

        if(project.ext.has('version') && project.ext.version) {
            pomVersion = project.ext.version
        }

        project.group = pomGroupId
        project.version = pomVersion

        LogUtil.log(project, "PioneerPlugin",
                "======maven configuration project: ${project.name} -- mavenUrl: ${mavenUrl} -- mavenName: ${mavenAccount} -- mavenPwd: ${mavenPwd}")
        LogUtil.log(project, "PioneerPlugin",
                "======maven configuration project: ${project.name} -- groupId: ${pomGroupId}:${pomArtifactId}:${pomVersion}")

        project.publishing {
            publications {
                mavenProduction(MavenPublication) {
                    //group,artifactId和version
                    groupId = pomGroupId
                    artifactId = pomArtifactId
                    version = pomVersion

                    from components.release

                    artifact project.tasks.sourceJar
                    artifact project.tasks.javadocJar

                    pom {
                        licenses {
                            license {
                                name = 'The Apache License, Version 2.0'
                                url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                            }
                        }
                    }
                }
            }

            repositories {
                maven {
                    url = artifact_version.endsWith('SNAPSHOT') ? mavenUrlSnapShot : mavenUrl
                    credentials {
                        username = mavenAccount
                        password = mavenPwd
                    }
                }
            }
        }
    }
}
