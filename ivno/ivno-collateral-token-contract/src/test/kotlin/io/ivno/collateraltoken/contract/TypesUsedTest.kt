package io.ivno.collateraltoken.contract

import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestationContract
import io.onixlabs.corda.bnms.contract.membership.MembershipContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.internal.packageName
import net.corda.core.internal.packageNameOrNull
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

class TypesUsedTest {
    @Test
    fun `Determine serializable classes`() {
        val contractClasses = listOf(
            DepositContract::class,
            RedemptionContract::class,
            TransferContract::class,
            MembershipContract::class,
            MembershipAttestationContract::class
        )
            .withSuperClasses()
            .withNestedCommandClasses()

        val stateClasses = listOf(
            Deposit::class,
            Redemption::class,
            Transfer::class,
            Membership::class,
            MembershipAttestation::class
        )
            .withSuperClasses()
            .withNestedCommandClasses()

        val commandClasses = listOf(
            DepositContract.Advance::class,
            DepositContract.Request::class,
            RedemptionContract.Advance::class,
            RedemptionContract.Request::class,
            TransferContract.Advance::class,
            TransferContract.Request::class,
            MembershipContract.Revoke::class,
            MembershipContract.Amend::class,
            MembershipContract.Issue::class
        )
            .withSuperClasses()
            .withNestedCommandClasses()

        val allClasses = contractClasses + stateClasses + commandClasses

        val used = allClasses.fold(mutableMapOf<KClass<*>, List<KClass<*>>>()) { acc, clazz ->
            acc[clazz] = clazz.typesUsedByVisibleProperties()
            acc
        }

        println("\n\nCORDAPP OVERVIEW:")
        println("\nCOMMANDS:")
        printTypesUsedByCorDapp(used, ::isCommand)
        println("\nSTATES:")
        printTypesUsedByCorDapp(used, ::isState)
        println("\nTYPES USED BY STATES AND COMMANDS:")
        println("\nCUSTOM TYPES:")
        printTypesUsedByCorDapp(used) { !isCoreType(it) && !isCommand(it) && !isState(it) && !isContract(it) }
        println("\nCORE TYPES (Java, Kotlin, Corda):")
        printTypesUsedByCorDapp(used, ::isCoreType)
    }

    private fun isContract(clazz: KClass<*>) = clazz.isSubclassOf(Contract::class)
    private fun isCommand(clazz: KClass<*>) = clazz.isSubclassOf(CommandData::class)
    private fun isState(clazz: KClass<*>) = clazz.isSubclassOf(ContractState::class)

    private fun isCoreType(clazz: KClass<*>) = clazz.qualifiedName.orEmpty().startsWith("java")
        || clazz.qualifiedName.orEmpty().startsWith("kotlin")
        || clazz.qualifiedName.orEmpty().startsWith("net.corda")

    private fun printTypesUsedByCorDapp(
        used: MutableMap<KClass<*>, List<KClass<*>>>,
        filter: (clazz: KClass<*>) -> Boolean = { _ -> true }
    ) {
        (used.flatMap { (_, classList) ->
            classList
        } + used.keys)
            .distinct()
            .filter { !it.java.isInterface && filter(it) }
            .sortedBy { it.toString() }
            .groupBy { it.java.packageNameOrNull }
            .forEach { (pckg, classes) ->
                when {
                    pckg == null || classes.size == 1 -> { classes.forEach { println(" - ${it.qualifiedName}") } }
                    else -> {
                        println(" - $pckg")
                        classes.forEach {
                            println("   - ${it.toString().replace("class $pckg.", "")}")
                        }
                    }
                }
            }
    }

    private fun List<KClass<*>>.withSuperClasses() = flatMap { clazz ->
        clazz.allSuperclasses.filterNot {
            it.qualifiedName?.startsWith("kotlin.") ?: false
                || it.qualifiedName?.startsWith("net.corda.core.contracts") ?: false
        } + clazz
    }.distinct()

    private fun List<KClass<*>>.withNestedCommandClasses() = flatMap { contract ->
        contract.nestedClasses.filter { it.isSubclassOf(CommandData::class) } + contract
    }
        .distinct()

    private fun KType.allTypeArgsAsKClassifiers(): List<KClass<*>> {
        return if (classifier is KClass<*>) {
            if (arguments.isNotEmpty()) {
                arguments.flatMap { arg ->
                    if (arg.type?.classifier is KClass<*>) {
                        arg.type?.let { it.allTypeArgsAsKClassifiers() + (it.classifier as KClass<*>) } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }.map { it } + (classifier!! as KClass<*>)
            } else {
                if (classifier != null) listOf((classifier!! as KClass<*>)) else emptyList()
            }.distinct()
        } else {
            emptyList()
        }
    }

    private fun KClass<*>.typesUsedByVisibleProperties(level: Int = 0): List<KClass<*>> {
        return memberProperties.filter { it.visibility == KVisibility.PUBLIC }.flatMap {
            val returnType = it.returnType
            val classifiers = returnType.allTypeArgsAsKClassifiers()

            val classifierUsed = classifiers.flatMap { classifier ->
                if (classifier.qualifiedName!!.startsWith("java.lang")
                    || classifier.qualifiedName!!.startsWith("kotlin")
                ) {
                    emptyList()
                } else {
                    classifier.typesUsedByVisibleProperties(level + 4)
                }

            }.distinct()

            classifiers + classifierUsed
        }
            .distinct()
            .filterNot {
                it.toString().startsWith("T")
                    || it.toString().startsWith("class kotlin.Any")
                    || it.toString().startsWith("class java.lang.Class")
            }
            .sortedBy { it.toString() }
    }
}
