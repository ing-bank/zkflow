/**
 * Source attribution:
 *
 * This class was adapted from the Corda Tokens SDK (https://github.com/corda/token-sdk),
 * to be able to annotate them with ZKP annotations and use them in tests as a representative example
 * of what we want to be able to support in ZKFlow.
 *
 * They are not part of the ZKFlow distribution, since they are part of a sample application only.
 */
package com.example.token.sdk

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

// Open classes may be extended provided no values are overridden.
// Otherwise, serialization breaks.
// see, https://github.com/Kotlin/kotlinx.serialization/issues/1792

@ZKP
@CordaSerializable
data class IssuedTokenType(
    val issuer: @EdDSA Party,
    val tokenType: TokenType
) : TokenType(tokenType.tokenIdentifier, tokenType.fractionDigits)
