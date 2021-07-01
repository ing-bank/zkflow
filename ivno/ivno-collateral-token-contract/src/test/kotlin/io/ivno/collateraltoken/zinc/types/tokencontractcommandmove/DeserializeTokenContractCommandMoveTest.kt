package io.ivno.collateraltoken.zinc.types.tokencontractcommandmove

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenContract
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.ExperimentalSerializationApi

class DeserializeTokenContractCommandMoveTest :
    DeserializationTestBase<DeserializeTokenContractCommandMoveTest, TokenContract.Command.Move>(
        { it.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTokenContractCommandMoveTest>()
    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    companion object {
        @JvmStatic
        fun testData() = listOf(
            TokenContract.Command.Move(),
            TokenContract.Command.Move(null)
        )
    }
}
