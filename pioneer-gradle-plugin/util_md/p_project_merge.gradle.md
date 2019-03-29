import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.manifmerger.XmlDocument
import com.android.utils.ILogger

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        //history version: 25.3.0
        classpath "com.android.tools.build:manifest-merger:26.2.1"
    }
}

def manifestSrcFiles = []

def logger = new ILogger() {
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
};

def manifestMergeHandler = { ->

    File moduleDir = new File("$projectDir/src");
    File[] pModuleDirs = moduleDir.listFiles();
    if (pModuleDirs == null || pModuleDirs.length == 0) {
        return;
    }

    pModuleDirs.each {
        if (it.isDirectory() && it.name.startsWith("p_")) {
            def dirs = ["main", "${MAVEN_MODULE_APP}"]
            // 遍历main和对应平台的目录
            dirs.each { dir ->
                def manifestPath = it.absolutePath + "/$dir/AndroidManifest.xml";
                def manifestSrcFile = new File(manifestPath);
                if (manifestSrcFile.exists() && !manifestSrcFiles.contains(manifestPath)) {
                    manifestSrcFiles << manifestPath
                }
            }
        }
    }

//    println '======manifestSrcFiles: ' + manifestSrcFiles
    if (manifestSrcFiles == null || manifestSrcFiles.isEmpty()) {
        return;
    }

    int size = manifestSrcFiles.size();
    File mainManifestFile = new File(manifestSrcFiles[size - 1]);

    ManifestMerger2.MergeType mergeType = ManifestMerger2.MergeType.APPLICATION
    XmlDocument.Type documentType = XmlDocument.Type.MAIN;
    ManifestMerger2.Invoker invoker = new ManifestMerger2.Invoker(mainManifestFile, logger, mergeType, documentType);
    for (int i = 0; i < size - 1; i++) {
        File microManifestFile = new File( manifestSrcFiles[i]);
        if (microManifestFile.exists()) {
            invoker.addLibraryManifest(microManifestFile)
        }
    }
    def mergingReport = invoker.merge()
    def moduleAndroidManifest = mergingReport.getMergedDocument(MergingReport.MergedManifestKind.MERGED)

//    println '======buildDir: ' + buildDir
    new File("$buildDir").mkdirs()
    def file = new File("$buildDir/AndroidManifest.xml")
    file.createNewFile()
    file.write(moduleAndroidManifest, "UTF-8")

    android.sourceSets.main.manifest.srcFile "$buildDir/AndroidManifest.xml"
}

// 这是点击Sync Now 或者Sync Project with Gradle Files编译时使用的
// 这种方式只是定义一个task的同时，执行一个闭包。如果真想定义一个有具体执行内容的task，使用doFirst或者doLast
task manifestMergeTask {
    manifestMergeHandler()
}

// 执行命令行时执行, 这个task比project.afterEvaluate还晚执行
preBuild.doFirst {
    manifestMergeHandler()
}