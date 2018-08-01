package com.github.apiechowicz.jacococore

import org.gradle.api.Plugin
import org.gradle.api.Project

class JacococorePlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.task('jacocoConsoleReport') {
            doLast {
                JacocoXmlParser.logReport("${target.buildDir}/reports/jacoco/test/jacocoTestReport.xml")
            }
        }
    }
}
