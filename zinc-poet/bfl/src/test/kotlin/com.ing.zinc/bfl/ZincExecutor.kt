package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincFileIfNotExists
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.util.ensureFile
import com.ing.zkflow.util.runCommand
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.writeText
import kotlin.time.measureTime

object ZincExecutor {
    private fun Path.generateWitness(content: String) {
        ensureDirectory("data")
            .ensureFile("witness.json")
            .writeText(content)
    }

    fun Path.generateWitness(elementName: String, init: BitWitnessBuilder.() -> Unit) {
        val bitWitness = BitWitnessBuilder().apply(init).build()
        generateWitness("{\"$elementName\" : [$bitWitness]}")
    }

    fun Path.generateWitness(init: JsonObjectBuilder.() -> Unit) {
        generateWitness(buildJsonObject(init).toString())
    }

    fun ZincFile.Builder.createImports(module: BflModule) {
        mod { this.module = module.getModuleName() }
        newLine()
        use { path = "${module.getModuleName()}::${module.id}" }
        use { path = "${module.getModuleName()}::${module.getSerializedTypeDef().getName()}" }
        newLine()
    }

    fun Path.runCommandAndLogTime(command: String, timeoutInSeconds: Long = 5): Pair<String, String> {
        val output: Pair<String, String>

        val time = measureTime {
            output = this.runCommand(command, timeoutInSeconds)
        }
        log.info("[$command] took $time")

        return output
    }

    private val log = Logger.getLogger(ZincExecutor::class.qualifiedName)

    private fun BflModule.toCodeGenerationOptions() = CodeGenerationOptions(
        listOf(
            WitnessGroupOptions("test", this)
        )
    )

    private fun CodeGenerationOptions?.orFromModule(module: BflModule): CodeGenerationOptions =
        this ?: module.toCodeGenerationOptions()

    fun Path.generateCircuitBase(module: BflModule, codeGenerationOptions: CodeGenerationOptions? = null) {
        val options = codeGenerationOptions.orFromModule(module)
        // generate Zargo.toml
        createZargoToml(this::class.simpleName!!)
        // generate consts.zn
        zincFileIfNotExists("$CONSTS.zn") {
            options.witnessGroupOptions.forEach {
                add(it.witnessSizeConstant)
            }
        }
        // generate src files
        module.allModules {
            zincSourceFile(this, options)
        }
    }

    fun Path.generateDeserializeCircuit(module: BflModule) {
        val options = module.toCodeGenerationOptions()
        generateCircuitBase(module, options)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            mod { this.module = CONSTS }
            newLine()
            module.allModules {
                createImports(this)
            }
            function {
                val witnessGroupOptions = options.witnessGroupOptions.first()
                name = "main"
                parameter { name = SERIALIZED; type = witnessGroupOptions.witnessType }
                returnType = module.toZincId()
                body = "${module.id}::${witnessGroupOptions.deserializeMethodName}($SERIALIZED, 0 as u24)"
            }
        }
    }

    fun Path.generateEqualsCircuit(struct: BflStruct) {
        generateCircuitBase(struct)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            struct.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "left"; type = struct.toZincId() }
                parameter { name = "right"; type = struct.toZincId() }
                returnType = ZincPrimitive.Bool
                body = "left.equals(right)"
            }
        }
    }

    fun Path.generateEmptyCircuit(struct: BflStruct) {
        generateCircuitBase(struct)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            struct.allModules {
                createImports(this)
            }
            function {
                name = "main"
                returnType = struct.toZincId()
                body = "${struct.id}::empty()"
            }
        }
    }

    fun Path.generateNewCircuit(struct: BflStruct, initializer: String) {
        generateCircuitBase(struct)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            struct.allModules {
                createImports(this)
            }
            function {
                name = "main"
                returnType = struct.toZincId()
                body = "${struct.id}::new($initializer)"
            }
        }
    }
}
