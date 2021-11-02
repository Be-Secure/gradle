/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve

import org.gradle.integtests.fixtures.AbstractHttpDependencyResolutionTest

class DependencyManagementResultsAsInputsIntegrationTest extends AbstractHttpDependencyResolutionTest {

    def setup() {
        settingsFile << """
            rootProject.name = 'root'
            include 'project-lib'
        """
        mavenRepo.module("org.external", "external-lib").publish()
        file('lib/file-lib.jar') << 'content'
    }

    def "can use #type as work input"() {
        given:
        buildFile << """
            import org.gradle.internal.component.external.model.ImmutableCapability

            abstract class TaskWithInput extends DefaultTask {

                @Input
                abstract Property<$type> getInput()

                @OutputFile
                abstract RegularFileProperty getOutputFile()

                @Inject
                abstract WorkerExecutor getWorkerExecutor()

                @TaskAction
                def action() {
                    workerExecutor.classLoaderIsolation().submit(TaskWithInputWorkAction, parameters -> {
                        parameters.workInput.set(input)
                        parameters.workOutputFile.set(outputFile)
                    })
                }
            }

            interface TaskWithInputWorkParameters extends WorkParameters {
                Property<$type> getWorkInput()
                RegularFileProperty getWorkOutputFile()
            }

            abstract class TaskWithInputWorkAction implements WorkAction<TaskWithInputWorkParameters> {
                @Override
                void execute() {
                    println(parameters.workInput.get())
                }
            }

            tasks.register("verify", TaskWithInput) {
                outputFile.set(layout.buildDirectory.file('output.txt'))
                input.set($factory)
            }
        """

        when:
        succeeds("verify", "-Dn=foo")

        then:
        executedAndNotSkipped(":verify")

        when:
        succeeds("verify", "-Dn=foo")

        then:
        skipped(":verify")

        when:
        succeeds("verify", "-Dn=bar")

        then:
        executedAndNotSkipped(":verify")

        where:
        type                           | factory
        // For ResolvedArtifactResult
        "Attribute"                    | "Attribute.of(System.getProperty('n'), String)"
        "Capability"                   | "new ImmutableCapability('group', System.getProperty('n'), '1.0')"
        "ComponentArtifactIdentifier"  | "null"
        "ComponentIdentifier"          | "null"
        "ResolvedVariantResult"        | "null"
        "AttributeContainer"           | "null"
        // For ResolvedComponentResult
        "ResolvedComponentResult"      | "null"
        "DependencyResult"             | "null"
        "ComponentSelector"            | "null"
        "ComponentSelectionReason"     | "null"
        "ComponentSelectionDescriptor" | "null"
        "ModuleVersionIdentifier"      | "null"
    }

    def "can use files from ResolvedArtifactResult as work input"() {
        given:
        buildFile << """
            project(':project-lib') {
                apply plugin: 'java'
            }
            configurations {
                compile
            }
            repositories {
                maven { url "${mavenRepo.uri}" }
            }
            dependencies {
                compile 'org.external:external-lib:1.0'
                compile project('project-lib')
                compile files('lib/file-lib.jar')
            }

            abstract class TaskWithFilesInput extends DefaultTask {

                @InputFiles
                abstract ConfigurableFileCollection getInputFiles()

                @OutputFile
                abstract RegularFileProperty getOutputFile()
            }

            tasks.register('verify', TaskWithFilesInput) {
                inputFiles.from(configurations.compile.incoming.artifacts.resolvedArtifacts.map { it.collect { it.file } })
                outputFile.set(layout.buildDirectory.file('output.txt'))
                doLast {
                    println(inputFiles.files)
                }
            }
        """

        def sourceFile = file("project-lib/src/main/java/Main.java")
        sourceFile << """
            class Main {}
        """.stripIndent()
        sourceFile.makeOlder()

        when:
        succeeds "verify"

        then:
        executedAndNotSkipped ":project-lib:jar", ":verify"

        when:
        succeeds "verify"

        then:
        skipped ":project-lib:jar", ":verify"

        when:
        sourceFile.text = """
            class Main {
                public static void main(String[] args) {}
            }
        """.stripIndent()
        succeeds "verify"

        then:
        executedAndNotSkipped ":project-lib:jar", ":verify"
    }

    def "can combine files and metadata from ResolvedArtifactResult as work input"() {
        given:
        buildFile << """
            project(':project-lib') {
                apply plugin: 'java'
            }
            configurations {
                compile
            }
            repositories {
                maven { url "${mavenRepo.uri}" }
            }
            dependencies {
                compile 'org.external:external-lib:1.0'
                compile project('project-lib')
                compile files('lib/file-lib.jar')
            }

            interface FilesAndMetadata {

                @InputFiles
                ConfigurableFileCollection getInputFiles()

                @Input
                SetProperty<Class<?>> getMetadata()
            }

            abstract class TaskWithFilesAndMetadataInput extends DefaultTask {

                private final FilesAndMetadata filesAndMetadata = project.objects.newInstance(FilesAndMetadata)

                @Nested
                FilesAndMetadata getFilesAndMetadata() {
                    return filesAndMetadata
                }

                @OutputFile
                abstract RegularFileProperty getOutputFile()
            }

            tasks.register('verify', TaskWithFilesAndMetadataInput) {
                def resolvedArtifacts = configurations.compile.incoming.artifacts.resolvedArtifacts
                filesAndMetadata.inputFiles.from(resolvedArtifacts.map { it.collect { it.file } })
                filesAndMetadata.metadata.addAll(resolvedArtifacts.map { it.collect { it.type } })
                outputFile.set(layout.buildDirectory.file('output.txt'))
                doLast {
                    println(filesAndMetadata.inputFiles.files)
                    println(filesAndMetadata.metadata.get())
                }
            }
        """

        def sourceFile = file("project-lib/src/main/java/Main.java")
        sourceFile << """
            class Main {}
        """.stripIndent()
        sourceFile.makeOlder()

        when:
        succeeds "verify"

        then:
        executedAndNotSkipped ":project-lib:jar", ":verify"

        when:
        succeeds "verify"

        then:
        skipped ":project-lib:jar", ":verify"

        when:
        sourceFile.text = """
            class Main {
                public static void main(String[] args) {}
            }
        """.stripIndent()
        succeeds "verify"

        then:
        executedAndNotSkipped ":project-lib:jar", ":verify"
    }
}