package com.github.apiechowicz.jacococore

import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.Node
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.xml.sax.SAXParseException

import javax.activation.UnsupportedDataTypeException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JacocoXmlParser {
    private static final Logger log = Logging.getLogger(JacocoXmlParser.class)
    private static final List<String> counterNames = ['INSTRUCTION', 'BRANCH', 'LINE', 'COMPLEXITY', 'METHOD', 'CLASS']
            .asImmutable()

    private final String inputFile
    private final int classNameColumnWidth
    private final int counterColumnPadding
    private final Map<String, Integer> counterColumnWidths
    private final int totalWidth
    private final String boldLine
    private final String dottedLine
    private final String line
    private final String tableHeader
    private final String notApplicable

    JacocoXmlParser(String inputFile, int classNameColumnWidth, int counterColumnPadding) {
        this.inputFile = inputFile
        this.classNameColumnWidth = classNameColumnWidth
        this.counterColumnPadding = counterColumnPadding
        this.counterColumnWidths = counterNames.collectEntries {
            [(it): calculateWidth(it)]
        }.asImmutable()
        this.totalWidth = classNameColumnWidth + counterColumnWidths.inject(0) { result, key, value -> result + value }
        this.boldLine = '=' * totalWidth
        this.dottedLine = ' .' * (totalWidth / 2)
        this.line = '-' * totalWidth
        this.tableHeader = createTableHeader()
        this.notApplicable = 'n/a'
    }

    private int calculateWidth(String string) {
        int minimalColumnLength = Math.max(string.length(), '100.0%'.length())
        minimalColumnLength + counterColumnPadding
    }

    private String createTableHeader() {
        StringBuffer out = new StringBuffer('Class name'.center(classNameColumnWidth))
        counterNames.each { out << it.toLowerCase().capitalize().padLeft(counterColumnWidths.get(it)) }
        out.toString()
    }

    void createConsoleReport() {
        final Path path = Paths.get(inputFile)
        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException("Input file '${inputFile.toString()}' was not found.")
        }
        if (!path.toString().endsWith('.xml')) {
            throw new UnsupportedDataTypeException('Input file must be Jacoco report in xml format.')
        }
        parseXml(path)
    }

    private void parseXml(Path path) throws SAXParseException {
        log.lifecycle('Xml file found. Generating console report for Jacoco.')
        GPathResult jacocoReport = getParser().parse(path.toFile())
        processPackageStats(jacocoReport)
        processProjectStats(jacocoReport)
    }

    private static XmlSlurper getParser() {
        final XmlSlurper parser = new XmlSlurper()
        parser.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
        parser
    }

    private void processPackageStats(GPathResult report) {
        printSectionHeader('Package coverage statistics:')
        report.childNodes()
                .findAll { nodeNameEquals(it as Node, 'package') }
                .sort { a, b -> getNodeName(a) <=> getNodeName(b) }
                .forEach { processPackageNode(it as Node) }
    }

    private void printSectionHeader(String title) {
        log.lifecycle(boldLine)
        log.lifecycle(title.center(totalWidth))
        log.lifecycle(boldLine)
    }

    private static boolean nodeNameEquals(Node it, String nodeName) {
        it.name() == nodeName
    }

    private static String getNodeName(Node node) {
        node.attributes().get('name')
    }

    private void processPackageNode(Node packageNode) {
        log.lifecycle('Package name: ' + getNodeName(packageNode))
        log.lifecycle(tableHeader)
        processClassNodes(packageNode)
        log.lifecycle(dottedLine)
        processPackageStats(packageNode)
        log.lifecycle(line)
    }

    private void processClassNodes(Node packageNode) {
        packageNode.childNodes()
                .findAll { nodeNameEquals(it as Node, 'class') }
                .sort { a, b -> getNodeName(a) <=> getNodeName(b) }
                .forEach { processClassNode(it as Node) }
    }

    private void processClassNode(Node classNode) {
        StringBuffer out = new StringBuffer(getClassName(classNode).padRight(classNameColumnWidth))
        appendCountersData(classNode.childNodes(), out)
        log.lifecycle(out.toString())
    }

    private static String getClassName(Node classNode) {
        getNodeName(classNode)
                .tokenize(FileSystems.getDefault().getSeparator())
                .last()
    }

    private void appendCountersData(Iterator<Node> nodeIterator, StringBuffer out) {
        counterNames.each { appendCounterData(nodeIterator, it, out) }
    }

    private void appendCounterData(Iterator<Node> nodeIterator, String counterName, StringBuffer out) {
        Node counterNode = nodeIterator.find { it.attributes().get('type') == counterName } as Node
        String counterValue = counterNode != null ? calculatePercentageValue(counterNode) : notApplicable
        out << counterValue.padLeft(counterColumnWidths.get(counterName))
    }

    private static String calculatePercentageValue(Node counterNode) {
        Map<String, String> counterAttributes = counterNode.attributes()
        int covered = counterAttributes.get('covered') as int
        int missed = counterAttributes.get('missed') as int
        double percentage = (100 * (covered / (missed + covered)) as double).round(2)
        "$percentage%"
    }

    private void processPackageStats(Node packageNode) {
        StringBuffer out = new StringBuffer('Package total:'.padRight(classNameColumnWidth))
        appendCountersData(packageNode.childNodes(), out)
        log.lifecycle(out.toString())
    }

    private void processProjectStats(GPathResult report) {
        printSectionHeader('Project coverage statistics:')
        log.lifecycle(tableHeader)
        StringBuffer out = new StringBuffer('Project total:'.padRight(classNameColumnWidth))
        appendCountersData(report.childNodes(), out)
        log.lifecycle(out.toString())
    }
}
