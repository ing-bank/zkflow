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
        metadata: ResolvedZKCommandMetadata,
    ): BflStruct {
        metadata.commandSimpleName // just to unclench linter
        return struct {
            // TODO implement private/mixed commands
            name = COMMAND_GROUP
            val commandTypeName = getCommandTypeName(metadata)
            field {
                name = commandTypeName.getCommandFieldName()
                type = struct {
                    name = commandTypeName
                    field {
                        name = "signers"
                        type = standardTypes.getSignerListModule(metadata.numberOfSigners)
                    }
                    isDeserializable = false
                }
            }
            isDeserializable = false
            // Import types used in the [generateFromSignersFunction] function
            addImport(standardTypes.signerModule)
            addImport(standardTypes.getSignerListModule(metadata.numberOfSigners))
            addFunction(generateFromSignersFunction(metadata))
        }
    }

    @Suppress("UnusedPrivateMember")
    private fun generateFromSignersFunction(
        metadata: ResolvedZKCommandMetadata,
    ) = zincFunction {
        val fieldConstructors = generateFieldConstructors(metadata)
        val fieldAssignments = "${getCommandFieldName(metadata)}: ${getCommandFieldName(metadata)},"
        name = FROM_SIGNERS
        parameter {
            name = "signers"
            type = zincArray {
                elementType = standardTypes.signerModule.toZincId()
                size = "${metadata.numberOfSigners}"
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

    private fun generateFieldConstructors(
        commandMetadata: ResolvedZKCommandMetadata,
    ): String {
        val fieldName = getCommandFieldName(commandMetadata)
        val fieldType = getCommandTypeName(commandMetadata)
        val signerListModule = standardTypes.getSignerListModule(commandMetadata.numberOfSigners).id
        return """
            let $fieldName: $fieldType = {
                let mut ${fieldName}_array: [${standardTypes.signerModule.id}; ${commandMetadata.numberOfSigners}] = [${standardTypes.signerModule.defaultExpr()}; ${commandMetadata.numberOfSigners}];
                for i in (0 as u8)..${commandMetadata.numberOfSigners} {
                    ${fieldName}_array[i] = signers[i];
                }
                $fieldType::new($signerListModule::list_of(${fieldName}_array))
            };
        """.trimIndent()
    }
}
