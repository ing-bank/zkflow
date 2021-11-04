package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflType.Companion.BITS_PER_BYTE
import com.ing.zinc.bfl.BflType.Companion.SERIALIZED_VAR
import com.ing.zinc.bfl.ZincExecutor.createImports
import com.ing.zinc.bfl.ZincExecutor.generateCircuitBase
import com.ing.zinc.bfl.ZincExecutor.runCommand
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enum
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.asciiString
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.byteArray
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.utfString
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.toByteBoundary
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalPathApi
class ComplexBflTest {
    private fun bigDecimal(integerSize: Int, fractionSize: Int) = struct {
        id = "BigDecimal_${integerSize}_$fractionSize"
        field {
            name = "sign"
            type = BflPrimitive.I8
        }
        field {
            name = "integer"
            type = byteArray(integerSize)
        }
        field {
            name = "fraction"
            type = byteArray(fractionSize)
        }
    }

    private fun amount(tokenType: BflType) = struct {
        id = "${tokenType.id}Amount"
        field {
            name = "quantity"
            type = BflPrimitive.I64
        }
        field {
            name = "display_token_size"
            type = bigDecimal(100, 20)
        }
        field {
            name = "token_type_hash"
            type = byteArray(32)
        }
        field {
            name = "token"
            type = tokenType
        }
    }

    private fun stateType() = enum {
        id = "StateType"
        variant("START")
        variant("BUSY")
        variant("END")
    }

    private fun publicKey() = struct {
        id = "PublicKey"
        field {
            name = "scheme_id"
            type = BflPrimitive.U32
        }
        field {
            name = "encoded"
            type = byteArray(44)
        }
    }

    private fun abstractParty() = struct {
        id = "Party"
        field {
            name = "name"
            type = option {
                innerType = asciiString(128)
            }
        }
        field {
            name = "owning_key"
            type = publicKey()
        }
    }

    private fun uniqueIdentifier() = struct {
        id = "UniqueIdentifier"
        field {
            name = "external_id"
            type = option {
                innerType = utfString(50)
            }
        }
        field {
            name = "id"
            type = BflPrimitive.U128
        }
    }

    private fun linearPointer() = struct {
        id = "LinearPointer"
        field {
            name = "pointer"
            type = uniqueIdentifier()
        }
        field {
            name = "class_name"
            type = asciiString(192)
        }
        field {
            name = "is_resolved"
            type = BflPrimitive.Bool
        }
    }

    private fun transaction() = struct {
        id = "Transaction"
        field {
            name = "state"
            type = stateType()
        }
        field {
            name = "amount"
            type = amount(linearPointer())
        }
        field {
            name = "subject"
            type = abstractParty()
        }
        field {
            name = "notary"
            type = abstractParty()
        }
    }

    @Test
    fun `Complex large object should deserialize correctly`(@TempDir tempDir: Path) {
        // val tempDir = Files.createTempDirectory(this::class.simpleName)
        tempDir.generateHashAndDeserializeCircuit(transaction())

        val (_, stderr) = tempDir.runCommand("zargo run", 10)

        stderr shouldBe ""
    }

    private fun Path.generateHashAndDeserializeCircuit(module: BflStruct) {
        val options = CodeGenerationOptions(
            listOf(
                WitnessGroupOptions(
                    "TransactionList",
                    module.bitSize.toByteBoundary(),
                    "((WITNESS_BIT_LENGTH - 1 as u24) / $BITS_PER_BYTE as u24 + 1 as u24) * $BITS_PER_BYTE as u24"
                ),
                WitnessGroupOptions("transaction", module),
            )
        )
        // generate src/consts.zn
        val constsFile = zincSourceFile("consts.zn", generateConstsFile(options, module))
        // generate base circuit
        generateCircuitBase(module, options)
        // generate src/main.zn
        generateMain(module, constsFile, options.witnessGroupOptions.first())
    }

    private fun Path.generateMain(
        module: BflStruct,
        constsFile: ZincFile,
        witnessGroupOptions: WitnessGroupOptions,
    ) {
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            use {
                path = "std::crypto::blake2s"
            }

            mod {
                this.module = "consts"
            }
            constsFile.getFileItems().asSequence()
                .filterIsInstance<ZincConstant>()
                .forEach {
                    use {
                        path = "consts::${it.getName()}"
                    }
                }
            newLine()
            function {
                name = "main"
                parameter {
                    name = SERIALIZED_VAR
                    type = witnessGroupOptions.witnessType
                }
                returnType = zincArray {
                    elementType = module.toZincId()
                    size = "TRANSACTION_COUNT"
                }
                body = """
                        let hash = blake2s($SERIALIZED_VAR);
                        // dbg!("{}", hash);
    
                        let mut transactions = [${module.id}::empty(); TRANSACTION_COUNT];
                        for k in (0 as u24)..TRANSACTION_COUNT {
                            let bit_offset = k * TRANSACTION_BIT_LENGTH;
                            transactions[k] = ${module.id}::${witnessGroupOptions.deserializeMethodName}($SERIALIZED_VAR, bit_offset);
                        }
                        transactions
                """.trimIndent()
            }
        }
    }

    private fun generateConstsFile(
        options: CodeGenerationOptions,
        module: BflStruct
    ) = zincFile {
        constant {
            name = "TRANSACTION_BIT_LENGTH"
            type = ZincPrimitive.U24
            initialization = "${module.bitSize} as u24"
        }
        constant {
            name = "TRANSACTION_COUNT"
            type = ZincPrimitive.U16
            initialization = "2 as u16"
            comment = "replace"
        }
        constant {
            name = "WITNESS_BIT_LENGTH"
            type = ZincPrimitive.U24
            initialization = "TRANSACTION_COUNT as u24 * TRANSACTION_BIT_LENGTH"
        }
        options.addConstants(this)
    }
}
