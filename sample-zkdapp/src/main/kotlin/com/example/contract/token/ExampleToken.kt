package com.example.contract.token

import com.example.token.sdk.AbstractFungibleToken
import com.example.token.sdk.IssuedTokenType
import com.example.token.sdk.TokenType
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.versioning.ZincUpgrade
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import org.intellij.lang.annotations.Language
import java.math.BigDecimal
import java.util.UUID

val EUR = TokenType("EUR", 2)
fun digitalEuroTokenType(issuer: Party) = IssuedTokenType(issuer, EUR)
fun digitalEuro(amount: Double, issuer: Party, holder: AnonymousParty) = digitalEuro(BigDecimal(amount), issuer, holder)
fun digitalEuro(amount: BigDecimal, issuer: Party, holder: AnonymousParty) =
    ExampleToken(EUR.tokenIdentifier, TokenDescriptor("Digital EUR", UUID.randomUUID()), Amount.fromDecimal(amount, digitalEuroTokenType(issuer)), owner = holder)

const val TOKEN_CODE_SIZE = 3

/*
 * All states are required to implement an interface that extends VersionedContractStateGroup.
 * This way ZKFlow can discover which state versions belong together, regardless of their class name.
 */
interface VersionedExampleToken : VersionedContractStateGroup, ContractState

/*
 * Any state class you want to use in a ZKFlow transaction should be annotated with @ZKP.
 * This ensures it is picked up by the ZKFlow compiler plugin and that ZKFlow can guarantee fixed length inputs
 * to the ZKP proving and operations.
 *
 * ZKFlow also needs to know how to serialize all types
 * used within @ZKP-annotated classes. A limited set of  core Corda and Java/Kotlin types are supported out of
 * the box, but you will always need to provide information about custom types.
 *
 * There are two ways to provide this information to ZKFlow:
 * 1) Directly annotate the declaration of a custom type with @ZKP and with all its composed types also annotated.
 * 2) If you can't annotate the declaration of a custom type, for example because it is provided by one of your dependencies,
 *    you can provide the class name of a surrogate with a @Via annotation on the type of the property. You can then annotate this
 *    surrogate class as normal.
 *
 * There are examples of both options in this token class.
 */
@CordaSerializable
@BelongsToContract(ExampleTokenContract::class)
@ZKP
data class ExampleToken(
    /*
     * For any collection, ZKFLow needs to know the maximum length it can have. If its contents are less than that
     * at runtime, it will be padded to that length. If it is more, it is a runtime exception. This way, collections
     * can be guaranteed to always have the same length, so that the non-turing complete ZKP toolchain can handle it.
     *
     * ZKFlow will also need to know the size of the collection's elements, so that it can calculate the total fixed size
     * of a fixed-length collection. This is done as described above, by annotating types with information from which
     * ZKFlow can determine their size.
     *
     * For convenience, ZKFlow understands a bit more about certain core types. This is true for String for example.
     * ZKFlow already knows what Strings can contain and will need to be told whether the String contains
     * ASCII or UTF-8 chars and how many of those there can be at most.
     * Alternative would have been to ask the user for max length in bytes, but we
     * deemed this hard to reason about and not very developer-friendly.
     *
     * In case of String, ZKFlow offers two annotations: @UTF8 and @ASCII.
     */
    val code: @ASCII(TOKEN_CODE_SIZE) String,

    /*
     * This is an example of a custom type introduced by us. It is not supported by ZKFlow out of the box,
     * so it needs to be annotated. We can use it here as-is, because we own it, and
     * we could therefore annotate its declaration with @ZKP. See the declaration for details.
     *
     * > IMPORTANT NOTE: while using nested custom types like this to create hierarchies is
     * > supported by ZKFlow, it can cause maintainability challenges in the context of ZKFlow's state versioning.
     * > It is recommended to flatten hierarchies as much as possible.
     * >
     * > For example, making a change to `TokenDescriptor` requires a new version of it. In order not to break existing
     * > `ExampleToken` versions that refer to it, they will have to point to the old version. A new version of `ExampleToken`
     * > will have to be created to use the new version of `TokenDescriptor`. This error-prone and hard to maintain: changes cascade
     * > quite easily when using deeper hierarchies, and it is hard to keep track of what needs a new version. Flattening hierarchies
     * > where possible makes these changes more explicit and easier to manage.
     */
    val descriptor: TokenDescriptor,

    /*
     * Both Amount and IssueTokenType are custom types and not supported by ZKFlow out of the box,
     * so they need to be annotated. In this case, they are third party types (from Corda and the Tokens SDK)
     * and we can't annotate them directly, so we need to use a surrogate to do that. If you have a look
     * at the surrogate, you will see that it is fully ZKP-annotated.
     */
    override val amount: @Via<AmountIssuedTokenTypeSurrogate> Amount<IssuedTokenType>,

    /*
     * `AnonymousParty` is a supported core type, we only need to tell ZKFlow which key algo is used.
     *
     * > IMPORTANT NOTE: ZKFlow does not support `AbstractParty` (or any abstract type for that matter).
     * > This is because ZKFlow needs to know the size of a type at compile time at all times and we can't determine this
     * > for types that may have different implementations at runtime.
     * > For this reason ZKFlow favors programming to implementations  instead of to interfaces.
     * > Note that generics are supported, as long as the declared type used is concrete. See field amount above for an example.
     */
    val owner: @EdDSA AnonymousParty
) : AbstractFungibleToken(), VersionedExampleToken {
    /*
     * This constructor is for versioning. It tells ZKFlow that this version of ExampleToken is the next version
     * up from the one mentioned as `previous`, i.e. `ExampleTokenV1`.
     *
     * This constructor is used to generate an upgrade command and a ZKP circuit for that command that enforces the smart contract rules
     * for a state upgrade transaction.
     *
     * It allows you to call `UpgradeStateFlow(oldState, ExampleToken::class)` for any version of ExampleToken,
     * as long as it is lower than the one you want to upgrade to.
     *
     * Note that upgrade constructors are required to be annotated with a @ZincUpgrade annotation.
     * This is because ZKFlow currently cannot determine without help how to upgrade
     * a state inside the ZKP circuit. This is because while ZKFlow *does* generate all Kotlin types for
     * you to use in the ZKP circuit, it does *not* parse any code from method bodies yet, such as an upgrade constructor
     * or smart contract rules. That means we have to write those ourselves in the language for the circuit.
     */
    @ZincUpgrade(
        // This is the circuit code that should behave identical to the constructor below: it should construct Self from the
        // previous version of this class. Fields will be identical to the Kotlin fields, except in snake case instead of camel case.
        // To help you determine which fields from Kotlin are mapped to which fields in the ZKP circuit, you can check the following files that
        // are generated by ZKFlow:
        // - The generated ZKP circuit sources for any command that uses this state. These are created during kotlin compilation.
        //    In this case, have a look at the `IssuePrivate` command in `build/zinc/issue_private/` or any of the other commands that use
        //    this state. In those directories, you can find the following:
        //    - `structure/module_outputs_example_token_transaction_component.txt`. Describes the structure of this transaction component.
        //      Types and fields should match 1:1 with the generated kotlin structure mentioned below.
        //    - `src/module_example_token.zn`: the generated Zinc source for this state class. Naming is always: `module_<<state_class_to_snake_case>>.zn`
        //      Within this file you can find what other generated Zinc types are called and they can be found in the same way in the same directory.
        // - `src/main/zkp/structure.json`. This file is generated by calling `./gradlew generateZkpStructure`. This describes the Kotlin
        //   structure of all components that can be found in a transaction.
        upgrade = """
            Self::new(
                // Not present in previous version, using empty string. Note that the type to use can be found in generated Zinc files.
                AsciiString3::empty(), 
                // Not present in previous version, using empty token descriptor. Note that the type to use can be found in generated Zinc files.
                TokenDescriptor::empty(), 
                previous.amount, 
                previous.owner
            )
        """,

        // This is an additional check we might want to do as part of the smart contract that is generated for the upgrade transaction.
        // By default, upgrade smart contracts do nothing in addition to checking that the upgrade succeeds.
        // In this case, we want to ensure that a token can only be upgraded if the current owner agrees and signs the upgrade transaction.
        additionalChecks = """assert!(ctx.signers.contains(input.owner.public_key), "Token owner must sign");"""
    )
    constructor(previous: ExampleTokenV1): this("", TokenDescriptor("", UUID(0, 0)), previous.amount, previous.owner)

    override val holder = owner

    override fun withNewHolder(newHolder: AnonymousParty): ExampleToken {
        return ExampleToken(code, descriptor, amount, newHolder)
    }

    fun withNewHolder(newHolder: AnonymousParty, amount: Double): ExampleToken {
        val decimalAmount = BigDecimal(amount)
        require(decimalAmount <= this.amount.toDecimal()) { "Can't increase amount when assigning a new holder" }
        return ExampleToken(code, descriptor, Amount.fromDecimal(decimalAmount, this.amount.token), newHolder)
    }
}

@CordaSerializable
@BelongsToContract(ExampleTokenContract::class)
@ZKP
data class ExampleTokenV1(
    override val amount: @Via<AmountIssuedTokenTypeSurrogate> Amount<IssuedTokenType>,
    val owner: @EdDSA AnonymousParty
) : AbstractFungibleToken(), VersionedExampleToken {
    override val holder = owner

    override fun withNewHolder(newHolder: AnonymousParty): ExampleTokenV1 = ExampleTokenV1(amount, newHolder)

    fun withNewHolder(newHolder: AnonymousParty, amount: Double): ExampleTokenV1 {
        val decimalAmount = BigDecimal(amount)
        require(decimalAmount <= this.amount.toDecimal()) { "Can't increase amount when assigning a new holder" }
        return ExampleTokenV1(Amount.fromDecimal(decimalAmount, this.amount.token), newHolder)
    }
}