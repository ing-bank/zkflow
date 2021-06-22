package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.dasl.contracts.v1.account.AccountAddress
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name

object AccountAddressSerializer: SurrogateSerializer<AccountAddress, AccountAddressSurrogate>(
    AccountAddressSurrogate.serializer(),
    { AccountAddressSurrogate(it.accountId, it.party) }
)

@Serializable
data class AccountAddressSurrogate(
    @FixedLength([ACCOUNT_ID_LENGTH])
    val accountId: String,
    val party: @Contextual CordaX500Name,
): Surrogate<AccountAddress> {
    override fun toOriginal(): AccountAddress {
        return AccountAddress(accountId, party)
    }

    companion object {
        const val ACCOUNT_ID_LENGTH = 20
    }
}
