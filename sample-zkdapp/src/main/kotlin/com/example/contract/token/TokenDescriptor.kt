package com.example.contract.token

import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import java.util.UUID

@ZKP
data class TokenDescriptor(
    val description: @UTF8(DESCRIPTION_LENGTH) String,
    val uuid: UUID
) {
    companion object {
        const val DESCRIPTION_LENGTH = 32
    }
}
