package com.galaxybruce.pioneer.aar

import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileTree

/**
 * @author bruce.zhang
 * @date 2019/1/16 09:41
 * @description aar库级联依赖
 * 调用方式：
 * 1.现在build.gradle android节点下调用rootProject.ext.setAARDirs(project)
 * 2.在build.gradle dependencies节点下调用rootProject.ext.addAARLibs(project, depModule)
 *
 * <p>
 * modification history:
 */
public class AARDependency {

    /**
     * 是否是module依赖方式
     * @return
     */
    static boolean depModuleSource(Project project) {
        return  "1".equals(Utils.getPropertiesValue(project, "DEPENDENCIES_MODULE"))
    }

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
                dirs list.toArray();
            }
        }
    }

    /**
     * 添加aar依赖库
     * @param dh
     * @param ft
     * @param depModuleSource
     * @return
     *
     * 调用方式：
     * dependencies {
     *  ...
     *  boolean depModule = rootProject.ext.depModuleSource();
     *  rootProject.ext.addAARLibs(project, depModule);
     * }
     */
    static void addAARLibs(Project project, boolean depModuleSource = false)
    {
        DependencyHandler dh = project.dependencies;
        ConfigurableFileTree ft = project.fileTree(dir: 'libs', include: '**/*.aar');
        ft.each { File f ->
            //default目录下的aar是一直要依赖的，aars目录下的aar是替代module的
            if(f.parentFile.name.equals('default') || !depModuleSource && f.parentFile.name.equals('aars')) {
                dh.add("api", [name: f.name.lastIndexOf('.').with { it != -1 ? f.name[0..<it] : f.name }, ext: 'aar'])
            }
        }
    }

}
