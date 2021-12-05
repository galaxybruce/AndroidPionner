package com.galaxybruce.pioneer.copy

import com.galaxybruce.pioneer.PioneerExtension
import com.galaxybruce.pioneer.git.GitUtil
import com.galaxybruce.pioneer.utils.LogUtil
import com.galaxybruce.pioneer.utils.Utils
import org.gradle.api.Project
import org.gradle.api.Task

public class ProjectCopyOutputManager {

    public static void copy(Project project) {
        // 监听task的创建
        project.tasks.whenTaskAdded { task ->
//                LogUtil.log("task", task.name)
            if(project.android.productFlavors.size() > 0) {
                project.android.productFlavors.all { flavor ->
                    LogUtil.log(project, "flavor", flavor.name)
                    copyByTask(project, task, flavor.name, true)
                }
            } else {
                copyByTask(project, task, "", false)
            }
        }
    }

    /**
     * 我只需要assembleFlavorNameRelease这个task（正式环境打包发布的情况下）才保存mapping.txt
     * 规则可以自己定义，我这边定义的比较宽泛
     * @param task
     */
    private static void copyByTask(Project project, Task task, String flavorName, boolean hasFlavor) {
        // 如果存在flavor的情况，只处理有flavor的命令，assembleRelease命令不处理
        if(hasFlavor && "assembleRelease".equals(task.name)) {
            return
        }
        if (task.name.startsWith("assemble") && task.name.endsWith("Release")) {
            PioneerExtension extension = Utils.getPioneerExtension(project)
            if(!extension.copyMappingEnabled) {
                return
            }
            task.doLast {
                copyMapping(project, flavorName)
            }
        }
    }

    private static void copyMapping(Project project, String flavorName) {
        LogUtil.log(project, "copy", "复制mapping.txt文件出来")

        boolean hasFlavor = flavorName != null && !"".equals(flavorName);
        String sourcePath = "${project.buildDir}" + '/outputs/mapping/'
        if(hasFlavor) {
            sourcePath += flavorName + '/release/'
        } else {
            sourcePath += 'release/'
        }
        String sourceName = 'mapping.txt'

        File sourceFile = new File(sourcePath, sourceName)
        if(!sourceFile.exists()) {
            return
        }

        String renamePath
        PioneerExtension extension = Utils.getPioneerExtension(project)
//        if(extension.mappingDir) {
//            renamePath = extension.mappingDir + '/' + project.rootProject.name.toLowerCase()
//        } else {
            renamePath = project.rootProject.projectDir.parent + '/AndroidMapping/' + project.rootProject.name.toLowerCase()
//        }
        String renameName = ("mapping_${hasFlavor ? flavorName + '_' : ''}${project.android.defaultConfig.versionName}.txt")

        File renameDir = new File(renamePath)
        if(!renameDir.exists()) {
            renameDir.mkdirs()
        }
        File renameFile = new File(renamePath, renameName)
        if(renameFile.exists()) {
            renameFile.delete()
        }

        LogUtil.log(project, "copy", "sourcePath " + sourcePath)
        LogUtil.log(project, "copy", "renamePath " + renameFile.absolutePath)

        project.copy {
            from sourcePath
            include sourceName
            into renamePath
            rename {
                renameName
            }
        }

        commit2Git(project, new File(renamePath).getParentFile(), extension.mappingRemoteUrl)
    }

    /**
     * 关联远程库
     * @param gitRoot
     * @param remoteUrl
     */
    private static void commit2Git(Project project, def gitRoot, def remoteUrl) {
        if (!GitUtil.isGitDir(gitRoot)) {
            GitUtil.init(gitRoot)
        }
        if(remoteUrl) {
            return
        }

        try {
            GitUtil.addRemote(gitRoot, remoteUrl)
        } catch (Exception e) {
            LogUtil.log(project, 'git', e.message)
        }

//        try {
//            GitUtil.addFiles(gitRoot, ".")
//        } catch (Exception e) {
//            LogUtil.log(project, 'git', e.message)
//        }
//        GitUtil.commit(parentFile, "commit file ${renameName}")
    }

}
