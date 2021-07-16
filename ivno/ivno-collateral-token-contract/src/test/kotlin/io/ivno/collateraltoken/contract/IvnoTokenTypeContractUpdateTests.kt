package io.ivno.collateraltoken.contract

import com.ing.zknotary.testing.dsl.VerificationMode
import io.dasl.contracts.v1.crud.CrudCommands
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Disabled("Re-enable once we have everything serializable and when we have zktransaction DSL")
class IvnoTokenTypeContractUpdateTests : ContractTest() {
    override val verificationMode = VerificationMode.RUN
    override val commandData = CrudCommands.Update

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
