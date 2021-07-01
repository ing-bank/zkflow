package io.ivno.collateraltoken.serialization

import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.serialization.bfl.SerializersModuleRegistry
import com.ing.zknotary.common.serialization.bfl.serializers.StateRefSerializer
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositContract
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipContract
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.corda.core.contracts.StateRef
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
        contextual(MembershipAttestationSerializer)

        polymorphic(AbstractClaim::class) {
            subclass(ClaimSerializer(PolymorphicSerializer(Any::class)))
        }

        polymorphic(Any::class) {
            subclass(Int::class, MyIntSerializer)
        }

        contextual(MembershipContractIssueSerializer)
    }

    init {
        loggerFor<IvnoSerializers>().info("Registering Ivno serializers")
        SerializersModuleRegistry.register(serializersModule)

        ContractStateSerializerMap.register(Deposit::class, 1, Deposit.serializer())

        // TODO This only works with Ints as inner types for identity and settings.
        ContractStateSerializerMap.register(Membership::class, 2, MembershipWithIntSerializer)

        CommandDataSerializerMap.register(DepositContract.Request::class, 3, DepositContract.Request.serializer())
        CommandDataSerializerMap.register(MembershipContract.Issue::class, 4, MembershipContractIssueSerializer)
    }
}

object MembershipContractIssueSerializer: KSerializer<MembershipContract.Issue> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("dummy", PrimitiveKind.BYTE)
    override fun deserialize(decoder: Decoder): MembershipContract.Issue = MembershipContract.Issue
    override fun serialize(encoder: Encoder, value: MembershipContract.Issue) {}
}
