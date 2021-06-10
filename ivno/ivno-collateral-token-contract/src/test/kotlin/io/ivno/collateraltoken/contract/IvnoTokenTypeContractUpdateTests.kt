package io.ivno.collateraltoken.contract

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Re-enable once we have everything serializable and when we have zktransaction DSL")
class IvnoTokenTypeContractUpdateTests : ContractTest() {

    @Test
    fun `On token type updating, the transaction must include the Update command`() {
//        services.zkLedger {
//            transaction {
//                fails()
//                command(keysOf(/**required signers*/), Update())
//                verifies()
//            }
//        }
    }
}
