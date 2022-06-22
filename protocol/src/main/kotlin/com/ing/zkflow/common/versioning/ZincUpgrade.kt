package com.ing.zkflow.common.versioning

import org.intellij.lang.annotations.Language

/**
 * Annotation specifying the [body] of a static zinc method on the enclosing type.
 * The argument name of this method is derived from the parameter name in the constructor.
 *
 * ```kotlin
 * interface MyType : VersionedContractStateGroup
 * @ZKP data class MyTypeV1(val a: Int) : MyType
 * @ZKP data class MyTypeV2(val a: Int, val b: Int) : MyType {
 *     @ZincUpgrade("Self::new(previous_version.a, 0 as i32)")
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
annotation class ZincUpgrade(@Language("Rust")val body: String)
