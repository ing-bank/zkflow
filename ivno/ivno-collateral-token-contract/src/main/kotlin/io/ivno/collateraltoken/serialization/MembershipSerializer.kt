package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Serializable
data class MembershipSurrogate<S: Any, T: Any>(
    val network: @Contextual Network,
    val holder: @Polymorphic AbstractParty,
    @FixedLength([IDENTITY_LENGTH])
    val identity: Set<@Polymorphic AbstractClaim<S>>,
    @FixedLength([SETTINGS_LENGTH])
    val settings: Set<@Serializable(with = SettingSerializer::class) Setting<T>>,
    val linearId: @Contextual UniqueIdentifier,
    val previousStateRef: @Contextual StateRef?
) : Surrogate<Membership> {
    override fun toOriginal() = Membership(network, holder, identity, settings, linearId, previousStateRef)

    companion object {
        const val IDENTITY_LENGTH = 2
        const val SETTINGS_LENGTH = 3

        fun <S: Any, T: Any> from(membership: Membership, identityClass: KClass<S>, settingsClass: KClass<T>) =
            with(membership) {
                MembershipSurrogate(
                    network,
                    holder,
                    identity.map {
                        when (it) {
                            is Claim -> Claim(it.property, identityClass.cast(it.value))
                            else -> TODO("BFL encoding is not supported yet for ${it::class}")
                        }
                    }.toSet(),
                    settings.map { Setting(it.property, settingsClass.cast(it.value)) }.toSet(),
                    linearId,
                    previousStateRef
                )
            }
    }
}

open class MembershipSerializer<S: Any, T: Any>(
    identitySerializer: KSerializer<S>,
    identityClass: KClass<S>,
    settingsSerializer: KSerializer<T>,
    settingsClass: KClass<T>,
) : SurrogateSerializer<Membership, MembershipSurrogate<S, T>>(
    MembershipSurrogate.serializer(identitySerializer, settingsSerializer),
    { MembershipSurrogate.from(it, identityClass, settingsClass) }
)

object MembershipWithIntSerializer:
    MembershipSerializer<Int, Int>(MyIntSerializer, Int::class, Int.serializer(), Int::class)

object MembershipWithStringSerializer:
    MembershipSerializer<String, String>(MyStringSerializer, String::class, String.serializer(), String::class)

object MembershipWithIntAndStringSerializer:
    MembershipSerializer<Int, String>(MyIntSerializer, Int::class, String.serializer(), String::class)