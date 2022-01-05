package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.indent
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS

class CommandGroupFactory(
    private val standardTypes: StandardTypes
) {
    companion object {
        internal const val COMMAND = "Command"
        internal const val COMMAND_GROUP = "CommandGroup"
        internal const val FROM_SIGNERS = "from_$SIGNERS"

        internal fun getCommandTypeName(metadata: ResolvedZKCommandMetadata) =
            "${metadata.commandKClass.simpleName!!}$COMMAND".replace("$COMMAND$COMMAND", COMMAND)

        private fun getCommandFieldName(metadata: ResolvedZKCommandMetadata) =
            getCommandTypeName(metadata).removeSuffix(COMMAND).camelToSnakeCase()

        private fun String.getCommandFieldName() = removeSuffix(COMMAND).camelToSnakeCase()
    }

    fun createCommandGroup(
        transactionMetadata: ResolvedZKTransactionMetadata,
    ): BflStruct {
        return struct {
            name = COMMAND_GROUP
            transactionMetadata.commands.map {
                struct {
                    name = getCommandTypeName(it)
                    field {
                        name = "signers"
                        type = standardTypes.getSignerListModule(it.numberOfSigners)
                    }
                    isDeserializable = false
                }
            }.forEach {
                field {
                    name = it.id.getCommandFieldName()
                    type = it
                }
            }
            isDeserializable = false
            // Import types used in the [generateFromSignersFunction] function
            addImport(standardTypes.signerModule)
            transactionMetadata.commands.forEach {
                addImport(standardTypes.getSignerListModule(it.numberOfSigners))
            }
            addFunction(generateFromSignersFunction(transactionMetadata))
        }
    }

    private fun generateFromSignersFunction(
        transactionMetadata: ResolvedZKTransactionMetadata,
    ) = zincFunction {
        val fieldConstructors = getFieldConstructors(transactionMetadata)
        val fieldAssignments = transactionMetadata.commands.joinToString("\n") {
            "${getCommandFieldName(it)}: ${getCommandFieldName(it)},"
        }
        name = FROM_SIGNERS
        parameter {
            name = "signers"
            type = zincArray {
                elementType = standardTypes.signerModule.toZincId()
                size = "${transactionMetadata.numberOfSigners}"
            }
        }
        returnType = Self
        body = """
            ${fieldConstructors.indent(12.spaces)}

            Self {
                ${fieldAssignments.indent(16.spaces)}
            }
        """.trimIndent()
        comment = "Construct $COMMAND_GROUP from an array with all $SIGNERS"
    }

    private fun getFieldConstructors(
        transactionMetadata: ResolvedZKTransactionMetadata,
    ): String {
        var signerCount = 0
        val fieldConstructors = transactionMetadata.commands.joinToString("\n") {
            val fieldName = getCommandFieldName(it)
            val fieldType = getCommandTypeName(it)
            val signerListModule = standardTypes.getSignerListModule(it.numberOfSigners).id
            val result = """
                let $fieldName: $fieldType = {
                    let mut ${fieldName}_array: [${standardTypes.signerModule.id}; ${it.numberOfSigners}] = [${standardTypes.signerModule.defaultExpr()}; ${it.numberOfSigners}];
                    for i in (0 as u8)..${it.numberOfSigners} {
                        ${fieldName}_array[i] = signers[i + $signerCount];
                    }
                    $fieldType::new($signerListModule::list_of(${fieldName}_array))
                };
            """.trimIndent()
            signerCount += it.numberOfSigners
            result
        }
        return fieldConstructors
    }
}
