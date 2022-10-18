package com.example.rpc

import com.example.contract.cbdc.digitalEuro
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test

@Suppress("UnusedPrivateMember")
class E2ERpcTest {

    private val user = "user1"
    private val password = "test"

    private val zk1: ZkdappTesterRpcClient = ZkdappTesterRpcClient("127.0.0.1:10102", user, password)
//    private val zk2: ZkdappTesterRpcClient = ZkdappTesterRpcClient("127.0.0.1:10202", user, password)
//    private val zk3: ZkdappTesterRpcClient = ZkdappTesterRpcClient("127.0.0.1:10302", user, password)

    init {
        zk1.waitForConnection()
//        zk2.waitForConnection()
//        zk3.waitForConnection()
    }

    @Test
    fun `End2End test with ZKP notary`() {

        zk1.create(digitalEuro(1.0, TestIdentity.fresh("Issuer").party, zk1.party().anonymise()))

//        val moveStx = zk1.move(createTx, zk2.party())
//
//        val moveBackStx = zk2.move(moveStx, zk1.party())
//
//        val finalMoveStx = zk1.move(moveBackStx, zk3.party())
//
//        finalMoveStx.tx.outputs.single()
    }
}
