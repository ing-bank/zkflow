package com.ing.zkflow.annotations.corda

import net.corda.core.crypto.Crypto

/*********************************************************
 * Corda specific annotations: [AnonymousParty], and [Party]
 * Enables annotations:
 * val anonymousParty: [AnonymousParty]    --> val @EdDSA [AnonymousParty]
 * val party: [Party]                      |--> val @EdDSA [Party]
 * (or override default [CordaX500Name])   └--> val @EdDSA @CordaX500Spec(ConversionProvider::class) [Party]
 *********************************************************/

/**
 * Corda-specific annotations to specify serialization parameters for different signature algorithms.
 * Unfortunately, values like [Crypto.EDDSA_ED25519_SHA512.schemeNumberID] cannot be used directly for id
 * when defining a corresponding signature specification, because they're contained in values constructed at compile time.
 */
annotation class SignatureSpec(val cordaSignatureId: Int)

@Target(AnnotationTarget.TYPE)
@Suppress("MagicNumber")
@SignatureSpec(1)
/**
 * Corresponds to [Crypto.RSA_SHA256]
 */
annotation class RSA

@Target(AnnotationTarget.TYPE)
@Suppress("MagicNumber", "ClassName")
@SignatureSpec(2)
/**
 * Corresponds to [Crypto.ECDSA_SECP256K1_SHA256]
 */
annotation class EcDSA_K1

@Target(AnnotationTarget.TYPE)
@Suppress("MagicNumber", "ClassName")
@SignatureSpec(3)
/**
 * Corresponds to [Crypto.ECDSA_SECP256R1_SHA256]
 */
annotation class EcDSA_R1

@Target(AnnotationTarget.TYPE)
@Suppress("MagicNumber")
@SignatureSpec(4)
/**
 * Corresponds to [Crypto.EDDSA_ED25519_SHA512]
 */
annotation class EdDSA

@Target(AnnotationTarget.TYPE)
@Suppress("MagicNumber")
@SignatureSpec(5)
/**
 * Corresponds to [Crypto.SPHINCS256_SHA256]
 */
annotation class Sphincs
