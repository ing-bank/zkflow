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

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class IssuedTokenType(
    val issuer: Party,
    val tokenType: TokenType
) : TokenType(tokenType.tokenIdentifier, tokenType.fractionDigits)
