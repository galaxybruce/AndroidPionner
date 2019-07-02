package com.galaxybruce.pioneer.utils


import org.gradle.api.Project

public class LogUtil {

    public static void log(Project project, String tag, def msg) {
        println "Pioneer xxxxxx ${tag}: ${msg}"
    }
}
