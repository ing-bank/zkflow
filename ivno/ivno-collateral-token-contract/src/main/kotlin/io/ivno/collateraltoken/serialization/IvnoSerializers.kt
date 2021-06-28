package io.ivno.collateraltoken.serialization

import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.serialization.bfl.SerializersModuleRegistry
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositContract
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.utilities.loggerFor

object IvnoSerializers {
    val serializersModule = SerializersModule {
        // TODO: Improve this temporary generic handling when kotlinx's handling for generic types is incorporated in BFL
        contextual(BigDecimalAmountSerializer(BogusSerializer))
        contextual(PermissionSerializer)
        contextual(RoleSerializer)
        contextual(TokenDescriptorSerializer)
        contextual(NetworkSerializer)
        contextual(IvnoTokenTypeSerializer)
        contextual(AccountAddressSerializer)
        contextual(AttestationPointerSerializer)
        contextual(TokenTransactionSummaryNettedAccountAmountSerializer)
        contextual(TokenTransactionSummaryStateSerializer)
        contextual(AttestationSerializer)
    }

    init {
        loggerFor<IvnoSerializers>().info("Registering Ivno serializers")
        SerializersModuleRegistry.register(serializersModule)

        ContractStateSerializerMap.register(Deposit::class, 1, Deposit.serializer())
        CommandDataSerializerMap.register(DepositContract.Request::class, 3, DepositContract.Request.serializer())
    }
}
