package io.ivno.collateraltoken.serialization

import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.serialization.ContractStateSerializerMap
import com.ing.zkflow.serialization.SerializersModuleRegistry
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositContract
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferContract
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.bnms.contract.membership.MembershipContract
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.AttestationContract
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.DummyCommandData

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
        contextual(TokenContractCommandMoveSerializer)
        contextual(AttestationSerializer)
        contextual(MembershipAttestationSerializer)

        polymorphic(AbstractClaim::class) {
            // TODO Discuss approach to polymorphics in AbstractClaims, for now we only support Int
            // subclass(ClaimSerializer(PolymorphicSerializer(Any::class)))
            subclass(ClaimSerializer(Int.serializer()))
        }

        polymorphic(Any::class) {
            subclass(Int::class, MyIntSerializer)
        }

        contextual(MembershipContractIssueSerializer)
        contextual(AttestationContractIssueSerializer)
        contextual(DummyCommandDataIssueSerializer)
    }

    init {
        loggerFor<IvnoSerializers>().info("Registering Ivno serializers")
        SerializersModuleRegistry.register(serializersModule)

        ContractStateSerializerMap.register(Deposit::class, 1, Deposit.serializer())
        ContractStateSerializerMap.register(Transfer::class, 9, Transfer.serializer())

        // TODO This only works with Ints as inner types for identity and settings.
        ContractStateSerializerMap.register(Membership::class, 2, MembershipWithIntSerializer)

        ContractStateSerializerMap.register(MembershipAttestation::class, 3, MembershipAttestationSerializer)

        ContractStateSerializerMap.register(IvnoTokenType::class, 4, IvnoTokenTypeSerializer)

        CommandDataSerializerMap.register(DepositContract.Request::class, 5, DepositContract.Request.serializer())
        CommandDataSerializerMap.register(MembershipContract.Issue::class, 6, MembershipContractIssueSerializer)
        CommandDataSerializerMap.register(AttestationContract.Issue::class, 7, AttestationContractIssueSerializer)
        CommandDataSerializerMap.register(DummyCommandData::class, 8, DummyCommandDataIssueSerializer)
        CommandDataSerializerMap.register(TransferContract.Request::class, 10, TransferContract.Request.serializer())
    }
}

object MembershipContractIssueSerializer : KSerializer<MembershipContract.Issue> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("dummy", PrimitiveKind.BYTE)
    override fun deserialize(decoder: Decoder): MembershipContract.Issue = MembershipContract.Issue
    override fun serialize(encoder: Encoder, value: MembershipContract.Issue) {}
}

object AttestationContractIssueSerializer : KSerializer<AttestationContract.Issue> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("dummy", PrimitiveKind.BYTE)
    override fun deserialize(decoder: Decoder): AttestationContract.Issue = AttestationContract.Issue
    override fun serialize(encoder: Encoder, value: AttestationContract.Issue) {}
}

object DummyCommandDataIssueSerializer : KSerializer<DummyCommandData> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("dummy", PrimitiveKind.BYTE)
    override fun deserialize(decoder: Decoder): DummyCommandData = DummyCommandData
    override fun serialize(encoder: Encoder, value: DummyCommandData) {}
}
