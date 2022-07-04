package com.ing.zkflow.common.versioning

import org.intellij.lang.annotations.Language

/**
 * Annotation specifying the body of a static zinc method on the enclosing type and any additional checks that should happen during upgrade.
 * An example of an important additional check would be specifying the required signers of this upgrade.
 * By default, this does nothing, meaning that anyone can sign an upgrade transaction.
 *
 * The argument name of the Self::new method is derived from the parameter name in the upgrade constructor.
 *
 * Note in addition to the entire CommandContext, which contains the relevant parts of the entire transaction
 * for this circuit, the following to variables are also defined in the upgrade function to make life easier when
 * writing any additional checks:
 * - input (the input ContractState, so in the example below MyTypeV1)
 * - output (the output ContractState, so in the example below MyTypeV2)
 *
 * ```kotlin
 * interface MyType : VersionedContractStateGroup, ContractState
 * @ZKP data class MyTypeV1(val a: Int) : MyType
 * @ZKP data class MyTypeV2(val a: Int, val b: Int) : MyType {
 *     @ZincUpgrade(
 *         upgrade = "Self::new(previous_version.a, 0 as i32)",
 *         additionalChecks = """
 *             assert!(ctx.signers.contains(input.owner.public_key), "Input owner must sign");
 *             """.trimIndent()
 *     )
 *     constructor(previousVersion: MyTypeV1) : this(previousVersion.a, 0)
 * }
 * ```
 *
 * ```rust
 * impl MyTypeV2 {
 *     fn upgrade_from(previous_version: MyTypeV1) -> Self {
 *         Self::new(previous_version.a, 0 as i32)
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class ZincUpgrade(@Language("Rust") val upgrade: String, @Language("Rust") val additionalChecks: String = "")
