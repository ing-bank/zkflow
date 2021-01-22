package com.ing.zknotary.ksp.generator.helpers

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir

/**
 * We need to do a 2-pass compilation:
 * 1st pass to let ksp generate all the sources,
 * 2nd pass to the compiler compile the generated sources.
 *
 * This is a a convenience object. This functionality
 * ideally must be a part of the testing harness,
 * see, https://github.com/tschuchortdev/kotlin-compile-testing/issues/72
 */
object KSPRuntimeCompiler {
    fun compile(compilation: KotlinCompilation): KotlinCompilation.Result {
        val pass1 = compilation.compile()
        require(pass1.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 1st pass: ${pass1.exitCode}\n${pass1.messages}"
        }

        val pass2 = KotlinCompilation().apply {
            sources = compilation.sources + compilation.kspGeneratedSourceFiles
        }.compile()

        require(pass2.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 2nd pass: ${pass2.exitCode}\n${pass2.messages}"
        }

        return pass2
    }

    private val KotlinCompilation.kspGeneratedSourceFiles: List<SourceFile>
        get() = kspSourcesDir.resolve("kotlin")
            .walk()
            .filter { it.isFile }
            .map { SourceFile.fromPath(it.absoluteFile) }
            .toList()
}
