package com.ing.zinc.bfl.generator

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.poet.ZincFile
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.util.ensureFile
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.notExists
import kotlin.io.path.writeText

object ZincGenerator {
    fun Path.createZargoToml(circuitName: String = "test-circuit", version: String = "0.1.0") {
        resolve("Zargo.toml")
            .createFile()
            .writeText(
                """
                    [circuit]
                    name = '$circuitName'
                    type = 'circuit'
                    version = '$version'
                """.trimIndent()
            )
    }

    fun Path.zincSourceFile(name: String, init: ZincFile.Builder.() -> Unit): ZincFile {
        val zincFile = ZincFile.Builder().apply(init).build()
        return zincSourceFile(name, zincFile)
    }

    fun Path.zincSourceFile(name: String, zincFile: ZincFile): ZincFile {
        ensureDirectory("src")
            .ensureFile(name)
            .writeText(
                zincFile.generate()
            )
        return zincFile
    }

    fun Path.zincSourceFile(module: BflModule, codeGenerationOptions: CodeGenerationOptions): ZincFile {
        return zincSourceFile("${module.getModuleName()}.zn", module.generateZincFile(codeGenerationOptions))
    }

    internal fun Path.zincFileIfNotExists(fileName: String, init: ZincFile.Builder.() -> Unit): Path {
        val file = ensureDirectory("src")
            .resolve(fileName)
        if (file.notExists()) {
            val zincFile = ZincFile.Builder().apply(init).build()
            file.createFile()
                .writeText(
                    zincFile.generate()
                )
        }
        return file
    }
}
