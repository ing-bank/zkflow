package io.ivno.collateraltoken.zinc.types.membership

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.serialization.ClaimSerializer
import io.ivno.collateraltoken.serialization.ClaimSurrogate
import io.ivno.collateraltoken.serialization.MembershipSerializer
import io.ivno.collateraltoken.serialization.NetworkSerializer
import io.ivno.collateraltoken.zinc.types.membership
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.corda.core.crypto.Crypto

class DeserializeMembershipTest :
    DeserializationTestBase<DeserializeMembershipTest, DeserializeMembershipTest.Data>(
        {
            it.data.toZincJson(
                networkEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                isNetworkAnonymous = false,
                networkScheme = Crypto.EDDSA_ED25519_SHA512,
                holderEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                isHolderAnonymous = false,
                holderScheme = Crypto.EDDSA_ED25519_SHA512,
                identityPropertyLength = ClaimSurrogate.PROPERTY_LENGTH,
                identityValueLength = IDENTITY_VALUE_LENGTH,
                settingsValueLength = SETTINGS_VALUE_LENGTH,
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeMembershipTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule {
        polymorphic(AbstractClaim::class) {
            subclass(ClaimSerializer(String.serializer()))
        }

        contextual(NetworkSerializer)
        contextual(MembershipSerializer(String.serializer(), String::class, String.serializer(), String::class))
    }

    @Serializable
    data class Data(@FixedLength([IDENTITY_VALUE_LENGTH, SETTINGS_VALUE_LENGTH]) val data: @Contextual Membership)

    companion object {
        const val IDENTITY_VALUE_LENGTH = 7
        const val SETTINGS_VALUE_LENGTH = 7

        @JvmStatic
        fun testData() = listOf(
            Data(membership),
        )
    }
}