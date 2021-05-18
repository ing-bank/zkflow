package com.ing.zknotary.gradle.zinc.template

/**
 * Marks a type as a Party, since there are multiple variants.
 *
 * This interface is used in [PartyAndReferenceTemplateParameters] to restrict the generic T.
 *
 * The different variants are:
 * - AbstractParty
 * - AnonymousParty
 * - Party
 */
interface PartyType : NamedType
