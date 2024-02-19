package com.galaxybruce.pioneer.manifest


import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.manifmerger.PlaceholderHandler
import com.android.manifmerger.XmlDocument
import com.android.utils.ILogger
import com.android.utils.Pair
import com.galaxybruce.pioneer.run_module.ProjectModuleManager
import com.galaxybruce.pioneer.utils.LogUtil
import com.google.common.collect.ImmutableList
import org.gradle.api.Project

import java.lang.reflect.Field

/**
 * 合并pin工程中的AndroidManifest.xml
 */
class ProjectManifestMerger {

    static boolean mergeManifest(Project project) {
        project.buildscript.repositories {
            maven { url 'https://maven.aliyun.com/repository/public'}
            maven { url "https://maven.aliyun.com/repository/google" }
            maven { url "https://maven.aliyun.com/repository/gradle-plugin" }
        }
//        project.buildscript.dependencies.add("classpath", "com.android.tools.build:manifest-merger:31.2.2")
        mergeManifest(project, true, true)
    }

    static void mergeManifest(Project project, boolean mergeAfterClean, boolean firstMerge) {
        List<String> manifestSrcFiles = new ArrayList<>()

        File moduleDir = new File("$project.projectDir/src")
        File[] moduleDirs = moduleDir.listFiles()
        if (moduleDirs == null || moduleDirs.length == 0) {
            return
        }

        def platformDir = PlatformSourceUtil.getPlatformFlag(project)
        LogUtil.log(project, "ProjectManifestMerger", "app platform source Dir: ${platformDir}")

        if(firstMerge) {
            project.android.sourceSets.main.jniLibs.srcDir("libs")
        }

        moduleDirs.each {
            if (it.isDirectory() && it.name.startsWith("p_")) {
                // pin工程下可以设置独立的build.gradle，但是该build.gradle中不允许apply plugin: 'com.android.library'
                if(firstMerge && new File("$project.projectDir/src/$it.name/build.gradle").exists()) {
                    project.apply from: "src/$it.name/build.gradle"
                }
//                // 添加pin工程下的libs依赖，pin/build.gradle中无需再添加 implementation fileTree(dir: 'src/p_lib1/libs', include: ['*.jar'])
//                project.dependencies.add("api",
//                        project.fileTree(dir: "src/$it.name/libs", include: ['*.jar']))

                def dirs = platformDir != null && platformDir.trim().length() > 0 ? ["main", "${platformDir}"] : ["main"]
                // 遍历main和对应平台的目录
                dirs.each { dir ->
                    if(dir != null && !dir.isEmpty()) {
                        if(project.file("${it.absolutePath}/$dir").exists()) {
                            LogUtil.log(project, "ProjectManifestMerger", "valid pin project resource dir: ${it.absolutePath}/$dir")
                            // manifest
                            def manifestPath = it.absolutePath + "/$dir/AndroidManifest.xml"
                            def manifestSrcFile = new File(manifestPath)
                            if (manifestSrcFile.exists() && !manifestSrcFiles.contains(manifestPath)) {
                                manifestSrcFiles << manifestPath
                            }

                            // 其他资源
                            if(firstMerge) {
                                project.android.sourceSets.main.java.srcDir("src/$it.name/$dir/java")
                                project.android.sourceSets.main.resources.srcDir("src/$it.name/$dir/resources")
                                project.android.sourceSets.main.res.srcDir("src/$it.name/$dir/res")
                                project.android.sourceSets.main.assets.srcDir("src/$it.name/$dir/assets")
                                project.android.sourceSets.main.jniLibs.srcDir("src/$it.name/$dir/libs")
//                            project.dependencies.add("api",
//                                    project.fileTree(dir: "src/$it.name/$dir/libs", include: ['*.jar']))
                            }
                        }
                    }
                }
            } else if (it.isDirectory() &&
                    (it.name == "main" ||
                            it.name == "${platformDir}" ||
                            // debug-test目录在module当做独立模块运行时生效
                            (it.name == ProjectModuleManager.DEBUG_DIR && project.ext.has('runAsApp') && project.ext.runAsApp))) {

                if(it.name == ProjectModuleManager.DEBUG_DIR && project.ext.has('runAsApp') && project.ext.runAsApp){
                    // debug-test目录下可以设置独立的build.gradle，但是该build.gradle中不允许apply plugin: 'com.android.library'
                    if (firstMerge && new File("$project.projectDir/src/$it.name/build.gradle").exists()) {
                        project.apply from: "src/$it.name/build.gradle"
                    }
                }

                LogUtil.log(project, "ProjectManifestMerger", "valid resource dir: ${it.absolutePath}")
                // pin工程以外的的情况，只处理main和platformDir两个目录
                // manifest
                def manifestPath = it.absolutePath + "/AndroidManifest.xml"
                def manifestSrcFile = new File(manifestPath)
                if (manifestSrcFile.exists() && !manifestSrcFiles.contains(manifestPath)) {
                    manifestSrcFiles << manifestPath
                }

                // 其他资源
                if(firstMerge) {
                    project.android.sourceSets.main.java.srcDir("src/$it.name/java")
                    project.android.sourceSets.main.resources.srcDir("src/$it.name/resources")
                    project.android.sourceSets.main.res.srcDir("src/$it.name/res")
                    project.android.sourceSets.main.assets.srcDir("src/$it.name/assets")
                    project.android.sourceSets.main.jniLibs.srcDir("src/$it.name/libs")
//                project.dependencies.add("api",
//                        project.fileTree(dir: "src/$it.name/libs", include: ['*.jar']))
                }
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
            if(intermediateManifestFile.exists() && !mergeAfterClean) {
                project.android.sourceSets.main.manifest.srcFile intermediateManifestFile.absolutePath
                LogUtil.log(project, "ProjectManifestMerger", "merged manifest exists!!!: ${intermediateManifestFile.absolutePath}")
                return
            }

            // 查找main manifest，其他的都是library manifest，只有main manifest中可以没有package，library的manifest中必须有package
            // 我们认为src/AndroidManifest.xml 或者 src/main/AndroidManifest.xml是main manifest
            final List<String> finalManifestSrcFiles = new ArrayList<>()
            String mainManifestFile = null
            for (manifest in manifestSrcFiles) {
                if(mainManifestFile == null && manifest.endsWith("src/AndroidManifest.xml")) {
                    mainManifestFile = manifest
                } else if(mainManifestFile == null && manifest.endsWith("src/main/AndroidManifest.xml")) {
                    mainManifestFile = manifest
                } else {
                    finalManifestSrcFiles.add(manifest)
                }
            }
            LogUtil.log(project, "ProjectManifestMerger", "mainManifestFile: ${mainManifestFile}")
            if(mainManifestFile == null) {
                throw new Exception("module[${project.name}] 缺少main AndroidManifest.xml，请添加下列任意一个文件src/AndroidManifest.xml 或者 src/main/AndroidManifest.xml")
            }

            ManifestMerger2.MergeType mergeType = ManifestMerger2.MergeType.FUSED_LIBRARY
            ManifestMerger2.Invoker manifestInvoker = ManifestMerger2.newMerger(new File(mainManifestFile), logger, mergeType)
            LogUtil.log(project, "ProjectManifestMerger", "namespace: ${project.android.namespace}, applicationId: ${project.rootProject.applicationId}")
            // 设置必要参数
            manifestInvoker.namespace = project.android.namespace
            manifestInvoker.setPlaceHolderValue(PlaceholderHandler.PACKAGE_NAME, project.rootProject.applicationId)
            manifestInvoker.asType(XmlDocument.Type.OVERLAY)

            try {
//                Class c = manifestInvoker.getClass()
//                Field f = c.getDeclaredField("mLibraryFilesBuilder")
//                f.setAccessible(true)

//                ImmutableList.Builder<Pair<String, File>> libraryFilesBuilder = new ImmutableList.Builder()
                for (manifest in finalManifestSrcFiles) {
                    File microManifestFile = new File(manifest)
                    if (microManifestFile.exists()) {
                        manifestInvoker.addLibraryManifest(microManifestFile)
//                        libraryFilesBuilder.add(Pair.of(microManifestFile.getName(), microManifestFile))
                    }
                }

//                f.set(manifestInvoker, libraryFilesBuilder)
            } catch (Exception e) {
                LogUtil.log(project, "ProjectManifestMerger", "add library manifest error: ${e.toString()}")
            }

            MergingReport mergingReport = manifestInvoker.merge()
            MergingReport.Result result = mergingReport.getResult()
            if(result.isSuccess()) {
                if(result.isWarning()) {
                    mergingReport.log(logger)
                }
                def moduleAndroidManifest = mergingReport.getMergedDocument(MergingReport.MergedManifestKind.MERGED)
                new File("$project.buildDir").mkdirs()
                def file = intermediateManifestFile // new File("$project.buildDir/AndroidManifest.xml")
                file.createNewFile()
                file.write(moduleAndroidManifest, "UTF-8")

                project.android.sourceSets.main.manifest.srcFile file.absolutePath
                LogUtil.log(project, "ProjectManifestMerger", "merged manifest success!!!: ${intermediateManifestFile.absolutePath}")
            } else {
                mergingReport.log(logger)
                throw new RuntimeException(mergingReport.getReportString());
            }
        }
    }

    private static ILogger logger = new ILogger() {
        @Override
        void error(Throwable t, String msgFormat, Object... args) {
            LogUtil.log("ProjectManifestMerger", "merge error: ${String.format(msgFormat, args)}")
        }

        @Override
        void warning(String msgFormat, Object... args) {
            LogUtil.log("ProjectManifestMerger", "merge warning: ${String.format(msgFormat, args)}")
        }

        @Override
        void info(String msgFormat, Object... args) {
            LogUtil.log("ProjectManifestMerger", "merge info: ${String.format(msgFormat, args)}")
        }

        @Override
        void verbose(String msgFormat, Object... args) {
//            LogUtil.log("ProjectManifestMerger", "merge verbose: ${String.format(msgFormat, args)}")
        }
    }
}