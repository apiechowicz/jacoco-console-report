package com.github.apiechowicz.jacococore

import org.gradle.api.Plugin
import org.gradle.api.Project

class JacococorePlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        def extension = target.extensions.create('jacococore', JacococorePluginExtension, target)

        target.task('jacocoConsoleReport') {
            doLast {
                def parser = new JacocoXmlParser(extension.inputFile, extension.classNameColumnWidth, extension.counterColumnPadding)
                parser.createConsoleReport()
            }
        }
    }
}
