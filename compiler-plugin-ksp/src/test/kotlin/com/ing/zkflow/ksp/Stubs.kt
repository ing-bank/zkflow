package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

data class KSNameStub(private val name: String) : KSName {
    override fun asString(): String = name
    override fun getQualifier(): String = name.split(".").dropLast(1).joinToString(".")
    override fun getShortName(): String = name.split(".").last()

    val simpleName: KSName by lazy { KSNameStub(getShortName()) }
    val packageName: KSName by lazy { KSNameStub(getQualifier()) }
}

data class CodeGeneratorStub(private val outputStream: ByteArrayOutputStream) : CodeGenerator {
    override val generatedFile: Collection<File> = emptyList()

    override fun associate(sources: List<KSFile>, packageName: String, fileName: String, extensionName: String) {
        // NOOP
    }

    override fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String
    ): OutputStream {
        return BufferedOutputStream(outputStream)
    }
}
