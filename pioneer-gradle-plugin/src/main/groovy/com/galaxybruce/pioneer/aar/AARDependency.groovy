package com.galaxybruce.pioneer.aar


import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileTree

/**
 * @author bruce.zhang
 * @date 2019/1/16 09:41
 * @description aar库级联依赖
 * 调用方式：
 * 1.在build.gradle android节点下调用rootProject.ext.setAARDirs(project)
 * 2.在build.gradle dependencies节点下调用rootProject.ext.addAARLibs(project, depModule)
 *
 */
public class AARDependency {

    /**
     * 设置aar库的path，把所有module中的aar都依赖
     * @param project
     *
     * 调用方式:
     * android {
     *  ...
     * rootProject.ext.setAARDirs(project);
     * }
     */
    static void setAARDirs(Project project) {
        project.repositories {
            List list = new ArrayList<String>();
            project.rootProject.getSubprojects().each {p ->
                list.add(p.projectDir.absolutePath + '/libs');
                list.add(p.projectDir.absolutePath + '/libs/aars');
                list.add(p.projectDir.absolutePath + '/libs/aars/default');
            }
            flatDir {
                //dirs 'libs','libs/aars','libs/aars/default'
                dirs list.toArray()
            }
        }
    }

    /**
     * 添加aar依赖库
     * @param dh
     * @param ft
     * @return
     *
     * 调用方式：
     * dependencies {
     *  ...
     *  boolean depModule = rootProject.ext.depModuleSource();
     *  rootProject.ext.addAARLibs(project, depModule);
     * }
     */
    static void addAARLibs(Project project) {
        DependencyHandler dh = project.dependencies
        ConfigurableFileTree ft = project.fileTree(dir: 'libs', include: '**/*.aar')
        ft.each { File f ->
            dh.add("api",
                    [name: f.name.lastIndexOf('.').with { it != -1 ? f.name[0..<it] : f.name }, ext: 'aar'])
        }
    }

}
