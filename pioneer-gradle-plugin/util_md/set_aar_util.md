/**
 * 编译相关脚本
 * created by bruce.zhang
 * /

/**
 * 是否是module依赖方式
 * @return
 */
def isDepModule()
{
    return  "1".equals(DEPENDENCIES_MODULE);
}

/**
 * 设置aar库的path
 * @param project
 *
 * 调用方式:
 * android {
 *  ...
 * rootProject.ext.setAARDirs(project);
 * }
 */
def setAARDirs(Project project)
{
    project.repositories {
        List list = new ArrayList<String>();
        rootProject.getSubprojects().each {p ->
            list.add(p.projectDir.absolutePath + '/libs');
            list.add(p.projectDir.absolutePath + '/libs/aars');
            list.add(p.projectDir.absolutePath + '/libs/aars/default');
        }
        flatDir {
            //dirs 'libs','libs/aars','libs/aars/default'
            dirs list.toArray();
        }
    }
}

/**
 * 添加aar依赖库
 * @param dh
 * @param ft
 * @param isDepModule
 * @return
 *
 * 调用方式：
 * dependencies {
 *  ...
 *  boolean depModule = rootProject.ext.isDepModule();
 *  rootProject.ext.addAARLibs(project, depModule);
 * }
 */
def addAARLibs(Project project, boolean isDepModule = false)
{
    DependencyHandler dh = project.dependencies;
    ConfigurableFileTree ft = project.fileTree(dir: 'libs', include: '**/*.aar');
    ft.each { File f ->
        //default目录下的aar是一直要依赖的，aars目录下的aar是替代module的
        if(f.parentFile.name.equals('default') || !isDepModule && f.parentFile.name.equals('aars'))
        {
            dh.add("api", [name: f.name.lastIndexOf('.').with { it != -1 ? f.name[0..<it] : f.name }, ext: 'aar'])
        }
    }
}

ext{
    isDepModule = this.&isDepModule         //是否是module依赖方式
    setAARDirs = this.&setAARDirs           //设置aar库的path
    addAARLibs = this.&addAARLibs           //添加aar依赖库
}


