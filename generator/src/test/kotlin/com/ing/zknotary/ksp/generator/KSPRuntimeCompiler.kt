package com.ing.zknotary.ksp.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir

object KSPRuntimeCompiler {
    fun compile(compilation: KotlinCompilation): KotlinCompilation.Result {
        val pass1 = compilation.compile()
        require(pass1.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 1st pass"
        }

        val pass2 = KotlinCompilation().apply {
            sources = compilation.sources + compilation.kspGeneratedSourceFiles
        }.compile()

        require(pass2.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 2nd pass"
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
