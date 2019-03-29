/**
 * 从local.properties获取library path
 *
 * 最好设置环境变量
 * export LIBRARY_PATH=/Users/bruce/work/sourcecode/Projects_Android/git/library/
 *
 * @author bruce.zhang
 */
def getLibraryPath() {

    final def pathKey = 'LIBRARY_PATH'
    def props = new Properties()
    def propFile = new File("local.properties")
    propFile.withInputStream {
        stream -> props.load(stream)
    }

    def libraryPath = props.getProperty(pathKey)
    if(!libraryPath) {
        libraryPath = System.getenv(pathKey)
        if(!libraryPath) {
            libraryPath = '/Users/.jenkins/workspace/malllib/'
        }
        props.put(pathKey, libraryPath)
        props.store(propFile.newWriter(), null)
    }

    return libraryPath
}

ext {
    // 通过这三种方式引用 settings.ext.libraryPath  rootProject.ext.libraryPath  libraryPath
    libraryPath = getLibraryPath()
    mavenScriptPath = libraryPath + 'buildsystem/maven.gradle'
}


