package io.ivno.collateraltoken.serialization

import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.serialization.bfl.SerializersModuleRegistry
import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSerializer
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositContract
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.utilities.loggerFor

object IvnoSerializers {
    init {
        loggerFor<IvnoSerializers>().info("Registering Ivno serializers")
        SerializersModuleRegistry.register(SerializersModule {
            contextual(BigDecimalAmountSerializer(LinearPointerSerializer))
            contextual(RoleSerializer)
        })

        ContractStateSerializerMap.register(Deposit::class, 1, Deposit.serializer())
        CommandDataSerializerMap.register(DepositContract.Request::class, 3, DepositContract.Request.serializer())
    }
}