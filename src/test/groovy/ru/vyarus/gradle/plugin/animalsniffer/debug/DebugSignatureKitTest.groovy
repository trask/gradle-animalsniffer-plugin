package ru.vyarus.gradle.plugin.animalsniffer.debug

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.animalsniffer.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 08.02.2023
 */
class DebugSignatureKitTest extends AbstractKitTest {

    def "Check signature build from classes"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer'
            }

            animalsnifferSignature {
                debug = true
                files sourceSets.main.output 
            }            

            repositories { mavenCentral()}
            dependencies {
                implementation "org.codehaus.mojo:animal-sniffer-annotations:1.14"
                implementation 'org.slf4j:slf4j-api:1.7.25'
            }
        """
        fileFromClasspath('src/main/java/ann/Sample.java', '/ru/vyarus/gradle/plugin/animalsniffer/java/ann/Sample.java')
        fileFromClasspath('src/main/java/valid/Sample.java', '/ru/vyarus/gradle/plugin/animalsniffer/java/valid/Sample.java')
//        debug()

        when: "run task"
        BuildResult result = run('animalsnifferSignature')

        then: "task successful"
        result.task(':animalsnifferSignature').outcome == TaskOutcome.SUCCESS
        clean(result.output).contains """
\tfiles:
\t\tbuild/classes/java/main
\t\tbuild/resources/main
"""
    }


    def "Check signature build from jars"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer'
            }

            animalsnifferSignature {
                debug = true
                files configurations.compileClasspath                    
            }

            repositories { mavenCentral()}
            dependencies {
                implementation 'junit:junit:4.12'
                implementation "org.codehaus.mojo:animal-sniffer-annotations:1.14"
            }

        """
//        debug()

        when: "run task"
        BuildResult result = run('animalsnifferSignature')

        then: "task successful"
        result.task(':animalsnifferSignature').outcome == TaskOutcome.SUCCESS
        clean(result.output).contains "files:\n\t\t"
        clean(result.output).contains "caches/modules-2/files-2.1/junit/junit/4.12/2973d150c0dc1fefe998f834810d68f278ea58ec/junit-4.12.jar"
        clean(result.output).contains "caches/modules-2/files-2.1/org.codehaus.mojo/animal-sniffer-annotations/1.14/775b7e22fb10026eed3f86e8dc556dfafe35f2d5/animal-sniffer-annotations-1.14.jar"
        clean(result.output).contains "caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar"
    }


    def "Check signature other signatures"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer'
            }

            animalsnifferSignature {
                debug = true
                files sourceSets.main.output
                signatures configurations.signature                    
            }

            repositories { mavenCentral()}
            dependencies {                
                signature 'org.codehaus.mojo.signature:java16-sun:1.0@signature'
                implementation 'org.slf4j:slf4j-api:1.7.25'
            }

        """
        fileFromClasspath('src/main/java/valid/Sample.java', '/ru/vyarus/gradle/plugin/animalsniffer/java/valid/Sample.java')
//        debug()

        when: "run task"
        BuildResult result = run('animalsnifferSignature')

        then: "task successful"
        result.task(':animalsnifferSignature').outcome == TaskOutcome.SUCCESS
        clean(result.output).contains """
\tsignatures:
\t\tjava16-sun-1.0.signature

\tfiles:
\t\tbuild/classes/java/main
\t\tbuild/resources/main
"""
    }


    def "Check include and exclude classes"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer'
            }

            animalsnifferSignature {
                debug = true
                files sourceSets.main.output                
                include 'valid.*'
                exclude 'invalid.*' 
            }            

            repositories { mavenCentral()}
            dependencies {
                implementation "org.codehaus.mojo:animal-sniffer-annotations:1.14"
                implementation 'org.slf4j:slf4j-api:1.7.25'
            }
        """
        fileFromClasspath('src/main/java/ann/Sample.java', '/ru/vyarus/gradle/plugin/animalsniffer/java/ann/Sample.java')
        fileFromClasspath('src/main/java/valid/Sample.java', '/ru/vyarus/gradle/plugin/animalsniffer/java/valid/Sample.java')
//        debug()

        when: "run task"
        BuildResult result = run('animalsnifferSignature')

        then: "task successful"
        result.task(':animalsnifferSignature').outcome == TaskOutcome.SUCCESS
        clean(result.output).contains """
\tfiles:
\t\tbuild/classes/java/main
\t\tbuild/resources/main

\tinclude:
\t\tvalid.*

\texclude:
\t\tinvalid.*
"""
    }


    def "Check signature copy output"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.animalsniffer'
            }           
                
            task sig(type: ru.vyarus.gradle.plugin.animalsniffer.signature.BuildSignatureTask) {
                signatures configurations.signature       
                mergeSignatures = false    
                debug = true     
            }    
                        
            repositories { mavenCentral()}
            dependencies {                
                signature 'org.codehaus.mojo.signature:java16-sun:1.0@signature'                
                signature 'net.sf.androidscents.signature:android-api-level-24:7.0_r2@signature'
            }

        """
//        debug()

        when: "run task"
        BuildResult result = run('sig')

        then: "task successful"
        result.task(':sig').outcome == TaskOutcome.SUCCESS
        clean(result.output).contains """No signature build required, simply copying signature:
\tjava16-sun-1.0.signature -> build/animalsniffer/sig/sig_!java16-sun-1.0.sig
No signature build required, simply copying signature:
\tandroid-api-level-24-7.0_r2.signature -> build/animalsniffer/sig/sig_!android-api-level-24-7.0_r2.sig
"""
    }

    private clean(String out) {
        return out.replace('\r', '').replace(File.separator, '/')
    }
}
