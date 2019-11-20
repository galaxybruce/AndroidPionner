package com.galaxybruce.pioneer.manifest

import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.manifmerger.XmlDocument
import com.android.utils.ILogger
import com.android.utils.Pair
import com.galaxybruce.pioneer.PioneerExtension
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import com.google.common.collect.ImmutableList
import org.gradle.api.Project

import java.lang.reflect.Field

/**
 * 合并pin工程中的AndroidManifest.xml
 */
class ProjectManifestMerger {

    static boolean mergeManifest(Project project) {
        project.buildscript.repositories {
            google()
            jcenter()
        }
        //history version: 25.3.0
        project.buildscript.dependencies.add("classpath", "com.android.tools.build:manifest-merger:26.4.1")

        mergeManifest(project, true)
    }

    static void mergeManifest(Project project, boolean needMerge) {
        def manifestSrcFiles = []

        File moduleDir = new File("$project.projectDir/src")
        File[] pModuleDirs = moduleDir.listFiles()
        if (pModuleDirs == null || pModuleDirs.length == 0) {
            return
        }

        def platformDir = null
        try {
            PioneerExtension extension = Utils.getPioneerExtension(project)
            if(extension.platformSourceDir) {
                platformDir = extension.platformSourceDir
            }
            if(!platformDir) {
                platformDir = project.rootProject.galaxybrucepioneer.platformSourceDir
            }
            if(!platformDir) {
                platformDir = project.rootProject.MAVEN_MODULE_APP
            }
        } catch (Exception e) {
            platformDir = ''
        }
        if(platformDir != null && !platformDir.isEmpty()) {
            LogUtil.log(project, "ProjectManifestMerger", "app platform source Dir: ${platformDir}")
        }
        project.android.sourceSets.main.jniLibs.srcDir("libs")

        pModuleDirs.each {
            if (it.isDirectory() && it.name.startsWith("p_")) {
//                def dirs = ["main", "${project.MAVEN_MODULE_APP}"]
                def dirs = ["main", "${platformDir}"]
                // 遍历main和对应平台的目录
                dirs.each { dir ->
                    if(dir != null && !dir.isEmpty()) {
                        if(project.file("${it.absolutePath}/$dir").exists()) {
                            LogUtil.log(project, "ProjectManifestMerger", "valid resource dir1: ${it.absolutePath}/$dir")
                            // manifest
                            def manifestPath = it.absolutePath + "/$dir/AndroidManifest.xml"
                            def manifestSrcFile = new File(manifestPath)
                            if (manifestSrcFile.exists() && !manifestSrcFiles.contains(manifestPath)) {
//                              println '======manifestPath: ' + manifestPath
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
                }
            } else if (it.isDirectory() && (it.name == "main" || it.name == "${platformDir}")) {
                LogUtil.log(project, "ProjectManifestMerger", "valid resource dir2: ${it.absolutePath}")
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

        // src目录下的manifest
        def manifestPath = "$project.projectDir/src/AndroidManifest.xml"
        def manifestSrcFile = new File(manifestPath)
        if (manifestSrcFile.exists() && !manifestSrcFiles.contains(manifestPath)) {
            manifestSrcFiles << manifestPath
            LogUtil.log(project, "ProjectManifestMerger", "valid root manifest: ${manifestPath}")
        }

        if (manifestSrcFiles == null || manifestSrcFiles.isEmpty()) {
            return
        }

        int size = manifestSrcFiles.size()
        LogUtil.log(project, "ProjectManifestMerger", "manifestSrcFiles.size: ${size}")
        if(size == 1) {
            project.android.sourceSets.main.manifest.srcFile "${manifestSrcFiles[0]}"
        } else {
            def intermediateManifestFile = new File("$project.buildDir/AndroidManifest.xml")
            if(intermediateManifestFile.exists() && !needMerge) {
                project.android.sourceSets.main.manifest.srcFile intermediateManifestFile.absolutePath
                LogUtil.log(project, "ProjectManifestMerger", "merged manifest exists!!!: ${intermediateManifestFile.absolutePath}")
                return
            }

            File mainManifestFile = new File(manifestSrcFiles[size - 1])
            ManifestMerger2.MergeType mergeType = ManifestMerger2.MergeType.LIBRARY
            ManifestMerger2.Invoker invoker = ManifestMerger2.newMerger(mainManifestFile, logger, mergeType)
            invoker.asType(XmlDocument.Type.LIBRARY)

            try {
                Class c = invoker.getClass()
                Field f = c.getDeclaredField("mLibraryFilesBuilder")
                f.setAccessible(true)

                ImmutableList.Builder<Pair<String, File>> libraryFilesBuilder = new ImmutableList.Builder()
                for (int i = 0; i < size - 1; i++) {
                    File microManifestFile = new File(manifestSrcFiles[i])
                    if (microManifestFile.exists()) {
//                        invoker.addLibraryManifest(microManifestFile)
                        libraryFilesBuilder.add(Pair.of(microManifestFile.getName(), microManifestFile))
                    }
                }

                f.set(invoker, libraryFilesBuilder)
            } catch (Exception e) {
                LogUtil.log(project, "ProjectManifestMerger", "add library manifest error: ${e.toString()}")
            }

            def mergingReport = invoker.merge()
            def moduleAndroidManifest = mergingReport.getMergedDocument(MergingReport.MergedManifestKind.MERGED)

//        println '======buildDir: ' + project.buildDir
            new File("$project.buildDir").mkdirs()
            def file = intermediateManifestFile // new File("$project.buildDir/AndroidManifest.xml")
            file.createNewFile()
            file.write(moduleAndroidManifest, "UTF-8")

            project.android.sourceSets.main.manifest.srcFile file.absolutePath
            LogUtil.log(project, "ProjectManifestMerger", "merged manifest success!!!: ${intermediateManifestFile.absolutePath}")
        }
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