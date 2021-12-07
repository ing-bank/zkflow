package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommand
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enum
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.asciiString
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.byteArray
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.utf8String
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.util.requireInstanceOf
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ComplexBflTest {
    private fun bigDecimal(integerSize: Int, fractionSize: Int) = struct {
        name = "BigDecimal_${integerSize}_$fractionSize"
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
        name = "${tokenType.id}Amount"
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
        name = "StateType"
        variant("START")
        variant("BUSY")
        variant("END")
    }

    private fun publicKey() = struct {
        name = "PublicKey"
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
        name = "Party"
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
        name = "UniqueIdentifier"
        field {
            name = "external_id"
            type = option {
                innerType = utf8String(50)
            }
        }
        field {
            name = "id"
            type = BflPrimitive.U128
        }
    }

    private fun linearPointer() = struct {
        name = "LinearPointer"
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
        name = "Transaction"
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
        tempDir.generateDeserializeCircuit(transaction())
        tempDir.generateWitness(SERIALIZED) {
            // state
            bytes(0, 0, 0, 1)
            // amount<LinearPointer>
            // amount.quantity
            bytes(0, 0, 0, 0, 0, 0, 0, 12)
            // amount.display_token_size
            // amount.display_token_size.sign
            bytes(1)
            // amount.display_token_size.integer
            // amount.display_token_size.integer.size
            bytes(0, 0, 0, 1)
            // amount.display_token_size.integer.values
            bytes(*IntArray(100) { 7 })
            // amount.display_token_size.fraction
            // amount.display_token_size.fraction.size
            bytes(0, 0, 0, 1)
            // amount.display_token_size.fraction.values
            bytes(*IntArray(20) { 8 })
            // amount.token_type_hash
            // amount.token_type_hash.size
            bytes(0, 0, 0, 32)
            // amount.token_type_hash.values
            bytes(*IntArray(32) { 9 })
            // amount.token
            // amount.token.pointer
            // amount.token.pointer.external_id
            // amount.token.pointer.external_id.has_value
            bits(1)
            // amount.token.pointer.external_id.value.size
            bytes(0, 2)
            // amount.token.pointer.external_id.value.values
            bytes(0, 97, 0, 97).bytes(*IntArray(96))
            // amount.token.pointer.id
            bytes(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10)
            // amount.token.class_name
            // amount.token.class_name.size
            bytes(0, 2)
            // amount.token.class_name.values
            bytes(97, 97).bytes(*IntArray(190))
            // amount.token.is_resolved
            bits(1)
            // and the rest
            bytes(*IntArray(364))
        }

        val (stdout, stderr) = tempDir.runCommand("zargo run", 10)

        stderr shouldBe ""

        val output = stdout.parseJson().jsonObject
        output.pathElement("state") shouldBe JsonPrimitive("1")
        output.pathElement("amount", "quantity") shouldBe JsonPrimitive("12")
        output.pathElement("amount", "display_token_size", "fraction", "size") shouldBe JsonPrimitive("1")
        output.pathElement("amount", "token_type_hash", "size") shouldBe JsonPrimitive("32")
        output.pathElement("amount", "token", "is_resolved") shouldBe JsonPrimitive(true)
    }
}

fun JsonObject.pathElement(vararg field: String): JsonElement {
    var previousField: String? = null
    return field.fold(this as JsonElement) { acc, fieldName ->
        val obj = acc.requireInstanceOf<JsonObject> {
            "Expected field $previousField to be an object"
        }
        previousField = fieldName
        requireNotNull(obj[fieldName]) {
            "Expected field $fieldName to be present"
        }
    }
}
