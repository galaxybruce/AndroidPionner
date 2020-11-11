package com.galaxybruce.pioneer.maven

import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import com.galaxybruce.pioneer.utils.runtime.ExecuteResult
import com.galaxybruce.pioneer.utils.runtime.RunTimeTask
import org.gradle.api.Project
import org.gradle.api.Task
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
                    if (mavenInfo != null && mavenInfo.modules != null && !mavenInfo.modules.isEmpty()) {
                        mavenInfo.modules.forEach(new Consumer<ModuleInfo>() {
                            @Override
                            void accept(ModuleInfo moduleInfo) {
                                def moduleName = moduleInfo.name
                                println ""
                                LogUtil.log(rootProject, "PioneerPlugin", "start upload module[$moduleName] ...")
                                // ./gradlew :module1:uploadArchives :module2:uploadArchives :module3:uploadArchives
                                // rootProject.project(":$moduleName").tasks['uploadArchives'].execute()

                                if (OperatingSystem.current().isWindows()) {
                                    // window下用process.waitFor会出现死锁
                                    def eo = new ByteArrayOutputStream()
                                    def so = new ByteArrayOutputStream()
                                    def result = rootProject.exec {
                                        commandLine 'gradlew.bat', ":$moduleName:uploadArchives"
                                        standardOutput so
                                        errorOutput eo
                                        // Gradle will by default throw an exception and terminate when receiving non-zero result codes from commands
                                        ignoreExitValue true
                                    }
                                    // https://stackoverflow.com/questions/45396268/gradle-exec-how-to-print-error-output
                                    if (result.exitValue != 0) {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName] upload maven fail !!!!!! ")
                                        println eo.toString()
                                    } else {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName] upload maven success !!! ")
                                    }
                                } else {
//                                    def cmd = "./gradlew"
//                                    def process = ("$cmd :$moduleName:uploadArchives").execute()
//                                    def strErr = new StringBuffer()
//                                    process.consumeProcessErrorStream(strErr)
//                                    def result = process.waitFor() // 这里会出现死锁

                                    String command = String.format("./gradlew clean :%s:uploadArchives", moduleName)
                                    ExecuteResult executeResult = RunTimeTask.executeCommand(command, Integer.MAX_VALUE)
                                    if (executeResult.exitCode != 0) {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName] upload maven fail !!!!!! ")
                                        println executeResult.toString()
                                    } else {
                                        LogUtil.log(rootProject, "PioneerPlugin", "module[$moduleName] upload maven success !!! ")
                                    }
                                }
                            }
                        })
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
        Task uploadArchives = project.tasks.findByName("uploadArchives")
//        Task st = project.tasks.findByName("sourcesJar")
        // 如果原来的library中有上传脚本就不添加了
        if(uploadArchives.repositories.size() > 0) {
            return
        }

        // todo 这里是否要排除flutter中的module

        // project.group和version先设置默认的
        final MavenInfo mavenInfo = Utils.getExtValue(project.rootProject, "mavenInfo")
        if(mavenInfo != null) {
            project.group = mavenInfo.group
            if(mavenInfo.version) {
                project.version = mavenInfo.version
            }
        }

        final ModuleInfo moduleInfo = mavenInfo?.getModuleInfo(project.name)
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
            project.ext.version = mavenInfo.version
            if(moduleInfo.version){
                project.ext.version = moduleInfo.version
            }


            if(!project.rootProject.galaxybrucepioneer.mavenScriptPath) {
                applyUploadDefaultMavenScript(project)
            } else {
                project.apply from: project.rootProject.galaxybrucepioneer.mavenScriptPath
            }
        }
    }

    private static void applyUploadDefaultMavenScript(Project project) {
        // 参考./gradle/upload_maven.gradle

        // 注意：app中不能设置多渠道，否则上传maven中没有aar!!!
        // 注意：app中不能设置多渠道，否则上传maven中没有aar!!!
        // 注意：app中不能设置多渠道，否则上传maven中没有aar!!!

        project.apply plugin: 'maven'

//        if (project.hasProperty("android")) { // Android libraries
//            task androidJavadocs(type: Javadoc) {
//                source = android.sourceSets.main.java.srcDirs
//                classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
//            }
//
//            task androidJavadocsJar(type: Jar, dependsOn: androidJavadocs) {
//                classifier = 'javadoc'
//                from androidJavadocs.destinationDir
//            }
//
//            task androidSourcesJar(type: Jar) {
//                classifier = 'sources'
//                from android.sourceSets.main.java.srcDirs
//            }
//
//            artifacts {
//                archives androidSourcesJar
//                //archives androidJavadocsJar 因为代码中的注释不规范
//            }
//
//        } else { // Java libraries
//            task sourcesJar(type: Jar, dependsOn: classes) {
//                classifier = 'sources'
//                from sourceSets.main.allSource
//            }
//
//            task javadocJar(type: Jar, dependsOn: javadoc) {
//                classifier = 'javadoc'
//                from javadoc.destinationDir
//            }
//
//            artifacts {
//                archives sourcesJar
//                //archives androidJavadocsJar 因为代码中的注释不规范
//            }
//        }

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

        if(project.ext.has('platformSourceDir') && project.ext.platformSourceDir) {
            pomArtifactId += '-' + project.ext.platformSourceDir
        }

        if(project.ext.has('version') && project.ext.version) {
            pomVersion = project.ext.version
        }

        project.group = pomGroupId
        project.version = pomVersion

        LogUtil.log(project, "PioneerPlugin", "======maven configuration project: ${project.name} -- mavenUrl: ${mavenUrl} -- mavenName: ${mavenAccount} -- mavenPwd: ${mavenPwd}")
        LogUtil.log(project, "PioneerPlugin", "======maven configuration project: ${project.name} -- groupId: ${pomGroupId}:${pomArtifactId}:${pomVersion}")

        project.uploadArchives {
            repositories {
                mavenDeployer {
                    pom.groupId = pomGroupId
                    pom.artifactId = pomArtifactId
                    pom.version = pomVersion

                    pom.project {
                        licenses {
                            license {
                                name 'The Apache Software License, Version 2.0'
                                url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                            }
                        }
                    }

                    if (localMaven) {
                        // deploy到本地仓库
                        // 注意：要放在第一行，不然会被线上的覆盖了
                        // 引用方式
                        // maven{ url rootProject.file("repo-local") }

                        // 或者 (注意：settings.gradle必须这样引用)
                        // def projectDir = settings.rootProject.projectDir
                        // def repoFile = new File(projectDir, 'repo-local')
                        // maven{ url repoFile }
                        repository(url: project.uri(project.rootProject.projectDir.absolutePath + '/repo-local'))
                    } else {
                        // 提交到远程服务器：
                        repository(url: mavenUrl) {
                            authentication(userName: mavenAccount, password: mavenPwd)
                        }

                        snapshotRepository(url: mavenUrlSnapShot) {
                            authentication(userName: mavenAccount, password: mavenPwd)
                        }
                    }
                }
            }
        }
    }
}
