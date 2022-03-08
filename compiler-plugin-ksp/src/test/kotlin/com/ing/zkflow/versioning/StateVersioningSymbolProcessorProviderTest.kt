package com.ing.zkflow.versioning

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.ing.zkflow.common.versioning.VersionedInterface
import com.ing.zkflow.ksp.implementations.ImplementationsProcessor
import com.ing.zkflow.ksp.implementations.ImplementationsSymbolProcessor
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.ing.zkflow.ksp.versioning.SortedStateFamiliesRegistry
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import net.corda.core.internal.readText
import org.junit.jupiter.api.Test
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class VersionedInterfaceProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val versionedInterfaceProviderProcessor = VersionedInterfaceProviderProcessor(environment.codeGenerator)
        return ImplementationsSymbolProcessor(
            environment.codeGenerator,
            listOf(
                versionedInterfaceProviderProcessor
            )
        )
    }
}

class VersionedInterfaceProviderProcessor(val codeGenerator: CodeGenerator) :
    ImplementationsProcessor<VersionedInterface> {
    override val interfaceClass = VersionedInterface::class
    override fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration {
        val declarations = implementations.map { it.declaration }
        val familyNames =
            declarations.filter { it.classKind == ClassKind.INTERFACE }.map { it.qualifiedName!!.asString() }.toSet()
        val stateDeclarations = declarations.filter { it.classKind == ClassKind.CLASS }
        val stateFamiliesMap = SortedStateFamiliesRegistry.buildSortedMap(familyNames, stateDeclarations)
        val log = stateFamiliesMap.flatMap { (stateName, orderedStateClasses) ->
            orderedStateClasses.map { "$stateName - $it" }
        }.joinToString("\n")
        if (log.isNotBlank()) {
            @Suppress("SpreadOperator")
            codeGenerator
                .createNewFile(
                    Dependencies(
                        aggregating = false,
                        *declarations.mapNotNull { it.containingFile }.toList().toTypedArray()
                    ),
                    "com.ing.zkflow",
                    "ZKPStateVersions",
                    "log"
                )
                .write(log.toByteArray())
        }
        return ServiceLoaderRegistration(interfaceClass, implementations.map { it.java.qualifiedName })
    }
}

class StateVersioningSymbolProcessorProviderTest {

    @Test
    fun `SortedStateFamiliesRegistry should correctly order state versions`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFile, outputStream)

        result.readLog() shouldBe """
            MyStateInterface - MyStateV1
            MyStateInterface - MyStateV2
            MyStateInterface - MyState
        """.trimIndent()

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    companion object {
        private fun KotlinCompilation.Result.readLog() =
            getLog().readText(StandardCharsets.UTF_8)

        private fun KotlinCompilation.Result.getLog() =
            Paths.get("${outputDirectory.absolutePath}/../ksp/sources/resources/com/ing/zkflow/ZKPStateVersions.log")

        @Suppress("UnusedPrivateMember")
        private inline fun <reified T : Any> KotlinCompilation.Result.getGeneratedMetaInfServices() =
            getMetaInfServicesPath<T>().readText(StandardCharsets.UTF_8)

        private inline fun <reified T : Any> KotlinCompilation.Result.getMetaInfServicesPath() =
            Paths.get("${outputDirectory.absolutePath}/../ksp/sources/resources/META-INF/services/${T::class.java.canonicalName}")

        private val kotlinFile = SourceFile.kotlin(
            "StateVersions.kt",
            """
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.VersionedInterface
                /**
                 * Annotation specifying the [body] of a zinc method on the enclosing type.
                 *
                 *
                 * ```rust
                 * impl MyTypeV2 {
                 *   fn upgradeFrom(previousVersion: MyTypeV1) -> Self {
                 *     [body]
                 *    }
                 * }
                 *
                 */
                @Target(AnnotationTarget.CONSTRUCTOR)
                annotation class ZincUpgradeMethod(val body: String)
                
                interface MyStateInterface: VersionedInterface
                @ZKP
                data class MyStateV1(
                    val name: @UTF8(8)  String
                ) : MyStateInterface
                
                
                @ZKP
                data class MyStateV2(
                    val firstName: @UTF8(8)  String,
                    val lastName: @UTF(8) String
                ) : MyStateInterface {
                    @ZincUpgradeMethod(""${'"'}${'"'}MyState::new(previousState.name,"")""${'"'})
                    constructor(previousState: MyStateV1) : this(previousState.name, "")
                }
                
                data class MyState(
                    val firstName: @UTF8(8)  String,
                    val lastName: @UTF(8) String,
                    val telNr: @UTF(10) String
                ): MyStateInterface {
                    @ZincUpgradeMethod(""${'"'}${'"'}MyState::new(previousState.firstName,previousState.lastName,"")""${'"'})
                    constructor(previousState: MyStateV2) : this(previousState.firstName, previousState.lastName, "")
                }
            """
        )
    }

    private fun compile(
        kotlinSource: SourceFile,
        outputStream: ByteArrayOutputStream
    ) = KotlinCompilation().apply {
        sources = listOf(kotlinSource)

        symbolProcessorProviders = listOf(VersionedInterfaceProvider())

        inheritClassPath = true
        messageOutputStream = BufferedOutputStream(outputStream) // see diagnostics in real time
    }.compile()

    private fun reportError(result: KotlinCompilation.Result, outputStream: ByteArrayOutputStream) =
        println(
            """
            Compilation failed:
            Compilation messages: ${result.messages}
            Output stream: $outputStream
            """.trimIndent()
        )
}
