package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.contracts.ZKCommandData
import net.corda.core.contracts.CommandData
import net.corda.core.internal.objectOrNewInstance
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf

@DslMarker
annotation class ZKTransactionMetadataDSL

@ZKTransactionMetadataDSL
class ZKCommandList : ArrayList<KClass<out CommandData>>() {
    class CommandNotObjectOrNoArgConstructorException(command: KClass<out CommandData>) :
        IllegalArgumentException("Command not supported: '$command'. $ERROR_COMMAND_NOT_OBJECT_OR_NOARG")

    class CommandNoMetadataFoundException(command: KClass<out CommandData>) :
        IllegalArgumentException("Command not supported: '$command'. $ERROR_COMMAND_NO_METADATA_FOUND")

    companion object {
        const val ERROR_COMMAND_NOT_UNIQUE = "Multiple commands of one type found. All commands in a ZKFLow transaction should be unique"
        const val ERROR_COMMAND_NOT_OBJECT_OR_NOARG =
            "ZKFlow only supports commands that are objects or that have a no arguments constructor"
        const val ERROR_COMMAND_NO_METADATA_FOUND = "No command metadata property found"
    }

    operator fun KClass<out CommandData>.unaryPlus() {
        require(!contains(this)) { ERROR_COMMAND_NOT_UNIQUE }
        add(this)
    }

    fun resolve(): List<ResolvedZKCommandMetadata> = map { kClass ->
        if (kClass.isSubclassOf(ZKCommandData::class)) {
            getMetadataFromMemberProperty(kClass)
        } else {
            /**
             * Metadata extension properties for non-ZKCommandData commands should be defined in
             * the companion object of one of the known commands in the command list.
             * That way, users can define metadata for commands that they use, without those commands having to support ZKFlow.
             */
            getMetadataFromExtensionProperty(kClass)
        }
    }

    private fun getMetadataFromMemberProperty(kClass: KClass<out CommandData>): ResolvedZKCommandMetadata {
        val command = getCommandInstance(kClass) as ZKCommandData
        return command.metadata
    }

    private fun getMetadataFromExtensionProperty(commandKClass: KClass<out CommandData>): ResolvedZKCommandMetadata {
        forEach { otherCommandKClass ->
            if (otherCommandKClass != commandKClass) {
                val metadataProperty = otherCommandKClass.companionObject?.declaredMemberExtensionProperties?.find { kProperty2 ->
                    kProperty2.name == ZKCommandData.METADATA_FIELD_NAME &&
                        kProperty2.returnType.classifier == ResolvedZKCommandMetadata::class &&
                        kProperty2.extensionReceiverParameter?.type?.classifier == commandKClass
                }
                val metadata =
                    metadataProperty
                        ?.getter?.call(
                            otherCommandKClass.companionObjectInstance,
                            getCommandInstance(commandKClass)
                        ) as? ResolvedZKCommandMetadata
                if (metadata != null) return metadata
            }
        }
        throw CommandNoMetadataFoundException(commandKClass)
    }

    /**
     * This instantiantes commands (again), causing the transaction metadata property itself to also be re-evaluated.
     * This causes a stack overflow. For this reason, we cache the resolved metadata.
     */
    private fun getCommandInstance(kClass: KClass<out CommandData>): CommandData {
        return try {
            kClass.objectOrNewInstance()
        } catch (e: IllegalArgumentException) {
            throw CommandNotObjectOrNoArgConstructorException(kClass)
        }
    }
}
