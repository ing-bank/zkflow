package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflWrappedTransactionComponent
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import net.corda.core.contracts.ComponentGroupEnum

/**
 * Used for components that do not need to be hashed, because they are publicly visible always.
 * That means the verifier can just directly provide their serialized form in the public input.
 * This saves an expensive hashing operation.
 */
internal class NonHashedArrayTransactionComponent(
    groupName: String,
    txComponent: BflWrappedTransactionComponent,
    groupSize: Int,
    componentGroup: ComponentGroupEnum
) : ArrayTransactionComponent(groupName, txComponent, groupSize, componentGroup) {
    /**
     * Doesn't actually calculate any hashes, but directly passes the serialized form through to the public input.
     */
    override val generateHashesMethod: ZincMethod = zincMethod {
        comment = "Passes directly through to the public input the serialized components of $groupName."
        name = "get_${groupName}_serialized_components"

        returnType = serializedType

        body = """
            self.$groupName
        """.trimIndent()
    }
}
