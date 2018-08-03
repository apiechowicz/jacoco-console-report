package com.github.apiechowicz.jacococore

import org.gradle.api.Project

class JacococorePluginExtension {
    private final String defaultJacocoXmlPath = '/reports/jacoco/test/jacocoTestReport.xml'

    String inputFile
    int classNameColumnWidth = 43
    int counterColumnPadding = 2

    JacococorePluginExtension(Project target) {
        inputFile = "${target.buildDir}" + defaultJacocoXmlPath
    }
}
