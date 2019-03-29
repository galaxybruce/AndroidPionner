package com.galaxybruce.pioneer.utils

import com.galaxybruce.pioneer.PioneerExtension
import org.gradle.api.Project

public class LogUtil {

    public static boolean logEnabled(Project project) {
        PioneerExtension extension = Utils.getPioneerExtension(project)
        return extension ? extension.logEnabled : true
    }

    public static void log(Project project, String tag, def msg) {
        if(logEnabled(project)) {
            println "Pioneer======${tag}: ${msg}"
        }
    }
}
