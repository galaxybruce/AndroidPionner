package com.galaxybruce.pioneer.manifest

import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.manifmerger.XmlDocument
import com.android.utils.ILogger
import com.galaxybruce.pioneer.PioneerExtension
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Project

/**
 * 合并pin工程中的AndroidManifest.xml
 */
class ProjectManifestMerger {

    static boolean mergeManifest(Project project, boolean addDependencies) {
        if(addDependencies) {
            project.buildscript.repositories {
                google()
                jcenter()
            }
            //history version: 25.3.0
            project.buildscript.dependencies.add("classpath", "com.android.tools.build:manifest-merger:26.2.1")
        }

        manifestMergeHandler(project)
    }

    private static void manifestMergeHandler(Project project) {
        def manifestSrcFiles = []

        File moduleDir = new File("$project.projectDir/src")
        File[] pModuleDirs = moduleDir.listFiles()
        if (pModuleDirs == null || pModuleDirs.length == 0) {
            return
        }

        def platformDir
        try {
            PioneerExtension extension = Utils.getPioneerExtension(project)
            if(extension.platformSourceDir) {
                platformDir = extension.platformSourceDir
            }
            if(!platformDir) {
                platformDir = project.rootProject.kidswantpioneer.platformSourceDir
            }
            if(!platformDir) {
                platformDir = project.MAVEN_MODULE_APP
            }
        } catch (Exception e) {
            platformDir = ''
        }
//        println '======MAVEN_MODULE_APP: ' + "${platformDir}"
        project.android.sourceSets.main.jniLibs.srcDir("libs")

        pModuleDirs.each {
            if (it.isDirectory() && it.name.startsWith("p_")) {
//                def dirs = ["main", "${project.MAVEN_MODULE_APP}"]
                def dirs = ["main", "${platformDir}"]
                // 遍历main和对应平台的目录
                dirs.each { dir ->
                    if(dir != null && !(''.equals(dir))) {
                        // manifest
                        def manifestPath = it.absolutePath + "/$dir/AndroidManifest.xml"
                        def manifestSrcFile = new File(manifestPath)
                        if (manifestSrcFile.exists() && !manifestSrcFiles.contains(manifestPath)) {
//                        println '======manifestPath: ' + manifestPath
                            manifestSrcFiles << manifestPath
                        }

                        // 其他资源
                        project.android.sourceSets.main.jniLibs.srcDir("src/$it.name/$dir/libs")
                        project.android.sourceSets.main.java.srcDir("src/$it.name/$dir/java")
                        project.android.sourceSets.main.resources.srcDir("src/$it.name/$dir/resources")
                        project.android.sourceSets.main.res.srcDir("src/$it.name/$dir/res")
                        project.android.sourceSets.main.assets.srcDir("src/$it.name/$dir/assets")
                        project.android.sourceSets.main.jniLibs.srcDir("src/$it.name/$dir/libs")
                    }
                }
            } else if (it.isDirectory() && (it.name == "main" || it.name == "${platformDir}")) {
                // pin工程以外的的情况，只处理main和platformDir两个目录
                // manifest
                def manifestPath = it.absolutePath + "/AndroidManifest.xml"
                def manifestSrcFile = new File(manifestPath)
                if (manifestSrcFile.exists() && !manifestSrcFiles.contains(manifestPath)) {
//                    println '======manifestPath: ' + manifestPath
                    manifestSrcFiles << manifestPath
                }

                // 其他资源
                project.android.sourceSets.main.jniLibs.srcDir("src/$it.name/libs")
                project.android.sourceSets.main.java.srcDir("src/$it.name/java")
                project.android.sourceSets.main.resources.srcDir("src/$it.name/resources")
                project.android.sourceSets.main.res.srcDir("src/$it.name/res")
                project.android.sourceSets.main.assets.srcDir("src/$it.name/assets")
                project.android.sourceSets.main.jniLibs.srcDir("src/$it.name/libs")
            }
        }

        if (manifestSrcFiles == null || manifestSrcFiles.isEmpty()) {
            return
        }

        int size = manifestSrcFiles.size()
        println '======manifestSrcFiles.size: ' + size
        File mainManifestFile = new File(manifestSrcFiles[size - 1])

        ManifestMerger2.MergeType mergeType = ManifestMerger2.MergeType.APPLICATION
        XmlDocument.Type documentType = XmlDocument.Type.MAIN;
        ManifestMerger2.Invoker invoker = new ManifestMerger2.Invoker(mainManifestFile, logger, mergeType, documentType)
        for (int i = 0; i < size - 1; i++) {
            File microManifestFile = new File( manifestSrcFiles[i])
            if (microManifestFile.exists()) {
                invoker.addLibraryManifest(microManifestFile)
            }
        }
        def mergingReport = invoker.merge()
        def moduleAndroidManifest = mergingReport.getMergedDocument(MergingReport.MergedManifestKind.MERGED)

//        println '======buildDir: ' + project.buildDir
        new File("$project.buildDir").mkdirs()
        def file = new File("$project.buildDir/AndroidManifest.xml")
        file.createNewFile()
        file.write(moduleAndroidManifest, "UTF-8")

        project.android.sourceSets.main.manifest.srcFile "$project.buildDir/AndroidManifest.xml"
    }

    private static ILogger logger = new ILogger() {
        @Override
        void error(Throwable t, String msgFormat, Object... args) {

        }

        @Override
        void warning(String msgFormat, Object... args) {

        }

        @Override
        void info(String msgFormat, Object... args) {

        }

        @Override
        void verbose(String msgFormat, Object... args) {

        }
    }
}