package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata

object CommandGroupFactory {
    internal const val COMMAND = "Command"
    internal const val COMMAND_GROUP = "CommandGroup"

    fun commandsGroup(
        transactionMetadata: ResolvedZKTransactionMetadata,
        signerModule: BflModule
    ): BflStruct {
        return struct {
            name = COMMAND_GROUP
            transactionMetadata.commands.map {
                struct {
                    name = getCommandTypeName(it)
                    field {
                        name = "signers"
                        type = getSignerListModule(it, signerModule)
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
        }
    }

    internal fun getSignerListModule(
        it: ResolvedZKCommandMetadata,
        signerModule: BflModule
    ) = list {
        capacity = it.numberOfSigners
        elementType = signerModule
    }

    internal fun getCommandTypeName(metadata: ResolvedZKCommandMetadata) =
        "${metadata.commandKClass.simpleName!!}$COMMAND".replace("$COMMAND$COMMAND", COMMAND)

    internal fun getCommandFieldName(metadata: ResolvedZKCommandMetadata) =
        getCommandTypeName(metadata).removeSuffix(COMMAND).camelToSnakeCase()

    internal fun String.getCommandFieldName() = removeSuffix(COMMAND).camelToSnakeCase()
}
