package ru.vyarus.gradle.plugin.animalsniffer.util

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.animalsniffer.report.ReportMessage

/**
 * Message parsing utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 19.03.2017
 */
@CompileStatic
class FormatUtils {
    private static final String DOT = '.'
    private static final String LINE_SEP = ':'
    private static final String NL = String.format('%n')
    private static final String UND_REF = 'Undefined reference:'
    private static final String FIELD_REF = ' Field '

    /**
     * Parse animasniffer ant task error.
     * Error format: <code>file_path:lineNum: Undefined reference: type source line</code>
     * <p>
     * In case of fields, it is impossible (for animalsniffer) to detect source line, instead, it would add field name
     * in the message: <code>file_path Field 'name': Undefined reference: type source line</code>
     *
     * @param message animalsniffer error
     * @param roots source directories (roots)
     * @return report message object
     */
    static ReportMessage parse(String message, Set<File> roots) {
        int msgStartIdx = message.indexOf(UND_REF)
        if (msgStartIdx < 0) {
            // try to look for first space (this will lead to wrong result if file path contain space)
            msgStartIdx = message.indexOf(' ')
        }
        String position = message[0..(msgStartIdx - 1)].trim()
        int fieldIdx = position.indexOf(FIELD_REF)
        String vclass
        String field = null
        if (fieldIdx > 0) {
            // -2 to cut off trailing ':'
            vclass = position[0..(fieldIdx - 1)].trim()
            field = position[fieldIdx + FIELD_REF.length()..-2].trim()
        } else {
            vclass = position
        }

        if (vclass.endsWith(LINE_SEP)) {
            // case when line number is not specified
            vclass = vclass[0..(vclass.length() - 2)]
        }
        String line = vclass.find(~/^(.+):(\d+)/) { match, file, line -> vclass = file; return line }
        vclass = extractJavaClass(vclass, roots)
        if (vclass == null) {
            return new ReportMessage(parseFail: true, code: message)
        }
        String code = message[msgStartIdx..-1]
        code = code.replace(UND_REF, '').trim()
        return new ReportMessage(source: vclass, line: line, field: field, code: code)
    }

    /**
     * @param msg message object
     * @param showSignature true to include signature name (used for multiple signatures)
     * @return error message formatted for file
     */
    static String formatForFile(ReportMessage msg, boolean showSignature) {
        if (msg.parseFail) {
            return msg.code
        }
        // file extension position
        int idx = msg.source.lastIndexOf(DOT)
        if (idx < 0) {
            idx = 0
        }
        String sig = showSignature ? " (${msg.signature})" : ''
        String srcLine = "${msg.source[0..(idx - 1)]}:${msg.line ? msg.line : 1}"
        if (msg.field) {
            srcLine += ' (#' + msg.field + ')'
        }
        return "$srcLine  Undefined reference$sig: ${msg.code}"
    }

    /**
     * @param msg message object
     * @param showSignature true to include signature name (used for multiple signatures)
     * @return error message formatted for console
     */
    static String formatForConsole(ReportMessage msg, boolean showSignature) {
        if (msg.parseFail) {
            return "[Unrecognized error] ${msg.code} $NL"
        }
        int clsIdx = -1
        int extIdx = msg.source.lastIndexOf(DOT)
        if (extIdx > 0) {
            clsIdx = msg.source[0..(extIdx - 1)].lastIndexOf(DOT)
        }
        String sig = showSignature ? " | ${msg.signature}" : ''
        // if can't find class fallback to simple format
        String srcLine = clsIdx > 0 ?
                "${msg.source[0..clsIdx]}(${msg.source[(clsIdx + 1)..-1]}:${msg.line ?: 1})" :
                "${msg.source}${msg.line ? LINE_SEP + msg.line : ''}"
        if (msg.field) {
            srcLine += " #$msg.field"
        }
        return "[Undefined reference$sig] $srcLine$NL" +
                "  >> ${msg.code}$NL"
    }

    /**
     * @param file absolute file path
     * @param root root folder path
     * @return class name (including package)
     */
    static String toClass(String file, String root) {
        String name = file[root.length() + 1..-1]
        name.replaceAll('\\\\|/', DOT)
    }

    @SuppressWarnings('ReturnNullFromCatchBlock')
    private static String extractJavaClass(String file, Set<File> roots) {
        String name
        try {
            name = new File(file).canonicalPath
        } catch (IOException ex) {
            // invalid path - do nothing
            return null
        }
        File root = roots.find { name.startsWith(it.canonicalPath) }
        if (!root) {
            return null
        }
        toClass(name, root.canonicalPath)
    }
}
