package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFileIfNotExists
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.util.ensureFile
import com.ing.zkflow.util.measureTimedValue
import com.ing.zkflow.util.runCommand
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.writeText

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
        add(module.mod())
        newLine()
        add(module.use())
        newLine()
    }

    fun Path.runCommandAndLogTime(command: String, timeoutInSeconds: Long = 5): Pair<String, String> {
        val timedValue = measureTimedValue {
            this.runCommand(command, timeoutInSeconds)
        }
        log.info("[$command] took ${timedValue.duration}")

        return timedValue.value
    }

    private val log = Logger.getLogger(ZincExecutor::class.qualifiedName)

    private fun BflModule.toCodeGenerationOptions() = CodeGenerationOptions(
        listOf(
            TransactionComponentOptions.wrapped("test", this)
        )
    )

    private fun CodeGenerationOptions?.orFromModule(module: BflModule): CodeGenerationOptions =
        this ?: module.toCodeGenerationOptions()

    fun Path.generateCircuitBase(module: BflModule, codeGenerationOptions: CodeGenerationOptions? = null) {
        val options = codeGenerationOptions.orFromModule(module)
        // generate Zargo.toml
        createZargoToml(this::class.simpleName!!)
        // generate consts.zn
        zincSourceFileIfNotExists("$CONSTS.zn") {
            options.transactionComponentOptions.forEach {
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
                val witnessGroupOptions = options.transactionComponentOptions.first()
                name = "main"
                parameter { name = SERIALIZED; type = witnessGroupOptions.witnessType }
                returnType = module.toZincId()
                body = module.deserializeExpr(witnessGroupOptions, "0 as u24", SERIALIZED, SERIALIZED)
            }
        }
    }

    fun Path.generateDeserializeWrappedTransactionComponentCircuit(module: BflWrappedTransactionComponent) {
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
                val witnessGroupOptions = options.transactionComponentOptions.first()
                name = "main"
                parameter { name = SERIALIZED; type = witnessGroupOptions.witnessType }
                returnType = module.lastField.type.toZincType()
                body = module.deserializeLastFieldExpr(witnessGroupOptions, "0 as u24", SERIALIZED)
            }
        }
    }

    fun Path.generateEqualsCircuit(module: BflModule) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "left"; type = module.toZincId() }
                parameter { name = "right"; type = module.toZincId() }
                returnType = ZincPrimitive.Bool
                body = module.equalsExpr("left", "right")
            }
        }
    }

    fun Path.generateEmptyCircuit(module: BflModule) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                returnType = module.toZincId()
                body = module.defaultExpr()
            }
        }
    }

    fun Path.generateNewCircuit(module: BflModule, vararg initializer: String) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                returnType = module.toZincId()
                body = "${module.id}::new(${initializer.joinToString { it }})"
            }
        }
    }
}
