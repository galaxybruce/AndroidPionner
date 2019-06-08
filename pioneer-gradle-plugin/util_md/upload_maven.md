apply plugin: 'maven'

if (project.hasProperty("android")) { // Android libraries
    task androidJavadocs(type: Javadoc) {
        source = android.sourceSets.main.java.srcDirs
        classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
    }

    task androidJavadocsJar(type: Jar, dependsOn: androidJavadocs) {
        classifier = 'javadoc'
        from androidJavadocs.destinationDir
    }

    task androidSourcesJar(type: Jar) {
        classifier = 'sources'
        from android.sourceSets.main.java.srcDirs
    }

    artifacts {
        archives androidSourcesJar
        //archives androidJavadocsJar 因为代码中的注释不规范
    }

} else { // Java libraries
    task sourcesJar(type: Jar, dependsOn: classes) {
        classifier = 'sources'
        from sourceSets.main.allSource
    }
    
    task javadocJar(type: Jar, dependsOn: javadoc) {
        classifier = 'javadoc'
        from javadoc.destinationDir
    }

    artifacts {
        archives sourcesJar
        //archives androidJavadocsJar 因为代码中的注释不规范
    }
}

if(project.hasProperty("MAVEN_MODULE_NAME") && MAVEN_MODULE_NAME) {
    def groupId = 'com.galaxybruce'
    def artifactId = MAVEN_MODULE_NAME

    if(project.ext.platformSourceDir) {
        artifactId = MAVEN_MODULE_NAME + project.ext.platformSourceDir
    }

    uploadArchives {
        repositories {
            mavenDeployer {
                repository(url: MAVEN_URL) {
                    authentication(userName: MAVEN_ACCOUNT_NAME, password: MAVEN_ACCOUNT_PWD)
                }

                pom.groupId = groupId
                pom.artifactId = artifactId
                pom.version = MODULE_VERSION

                pom.project {
                    licenses {
                        license {
                            name 'The Apache Software License, Version 2.0'
                            url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                        }
                    }
                }
            }
        }
    }
}