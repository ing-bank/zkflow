package com.example.token.cbdc

import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import net.corda.core.contracts.TokenizableAssetInfo
import java.math.BigDecimal

@ZKP
open class TokenType(
    /**
     * All [TokenType]s must have a [tokenIdentifier], which is typically a 3-4 character, upper case alphabetic string.
     * The [tokenIdentifier] is used in conjunction with the [tokenClass] to create an instance of a [TokenType], for
     * example: (FiatCurrency, GBP), (DigitalCurrency, BTC), or (Stock, GOOG). For [TokenPointer]s this property will
     * contain the linearId of the [EvolvableTokenType] which is pointed to. The linearId can be used to obtain the
     * underlying [EvolvableTokenType] from the vault.
     */
    open val tokenIdentifier: @UTF8(TOKEN_IDENTIFIER_LENGTH) String,
    /**
     * The number of fractional digits allowable for this token type. Specifying "0" will only allow integer amounts of
     * the token type. Specifying "2", allows two decimal places, much like most fiat currencies, and so on...
     */
    open val fractionDigits: Int,
) : TokenizableAssetInfo {
    companion object {
        const val TOKEN_IDENTIFIER_LENGTH: Int = 10
    }

    /**
     * For use by the [Amount] class. There is no need to override this.
     */
    override val displayTokenSize: BigDecimal get() = BigDecimal.ONE.scaleByPowerOfTen(-fractionDigits)

    /**
     * This method to allow simpler testing.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TokenType

        if (tokenIdentifier != other.tokenIdentifier) return false
        if (fractionDigits != other.fractionDigits) return false

        return true
    }

    /**
     * Good practice: it is explained in Effective Java 3rd edition. If you don't do this,
     * you may get unexpected behaviour while ordering, comparing and sorting in hash tables and such.
     */
    override fun hashCode(): Int {
        var result = tokenIdentifier.hashCode()
        result = 31 * result + fractionDigits
        return result
    }
}
