package com.ing.zknotary.testing

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

public infix fun KClass<*>.shouldHaveSamePublicApiAs(expected: KClass<*>) {
    val actualMemberFunctions = this.memberFunctions.filter { it.visibility == KVisibility.PUBLIC }
    val expectedMemberFunctions = expected.memberFunctions.filter { it.visibility == KVisibility.PUBLIC }

    expectedMemberFunctions.forEach { expectedFunction ->
        actualMemberFunctions.singleOrNull {
            it.name == expectedFunction.name &&
                parametersMatch(expectedFunction.parameters, it.parameters) &&
                returnTypesMatch(expected, expectedFunction.returnType, this, it.returnType)
        } ?: error("Public function $expectedFunction not present on $this")
    }

    val actualMemberProperties = this.memberProperties.filter { it.visibility == KVisibility.PUBLIC }
    val expectedMemberProperties = expected.memberProperties.filter { it.visibility == KVisibility.PUBLIC }

    expectedMemberProperties.forEach { expectedProperty ->
        actualMemberProperties.singleOrNull {
            it.name == expectedProperty.name &&
                parametersMatch(expectedProperty.parameters, it.parameters) &&
                returnTypesMatch(expected, expectedProperty.returnType, this, it.returnType)
        } ?: error("Public property $expectedProperty not present on $this")
    }

    val actualConstructors = this.constructors.filter { it.visibility == KVisibility.PUBLIC }
    val expectedConstructors = expected.constructors.filter { it.visibility == KVisibility.PUBLIC }

    expectedConstructors.forEach { expectedConstructor ->
        actualConstructors.singleOrNull {
            it.name == expectedConstructor.name &&
                parametersMatch(expectedConstructor.parameters, it.parameters) &&
                returnTypesMatch(expected, expectedConstructor.returnType, this, it.returnType)
        } ?: error("Constructor $expectedConstructor not present on $this")
    }
}

public fun parametersMatch(expected: List<KParameter>, actual: List<KParameter>): Boolean {
    val actualNoThis = actual.filter { it.kind != KParameter.Kind.INSTANCE }
    val expectedNoThis = expected.filter { it.kind != KParameter.Kind.INSTANCE }

    if (actualNoThis.size == expectedNoThis.size) {
        expectedNoThis.forEachIndexed { index, kParameter ->
            if (!paramMatches(kParameter, actualNoThis[index])) {
                return false
            }
        }
        return true
    }
    return false
}

public fun paramMatches(
    expected: KParameter,
    actual: KParameter
): Boolean {
    return actual.name == expected.name &&
        actual.index == expected.index &&
        actual.type == expected.type &&
        actual.kind == expected.kind &&
        actual.isOptional == expected.isOptional &&
        actual.isVararg == expected.isVararg
}

public fun returnTypesMatch(
    expectedKlazz: KClass<*>,
    expectedReturnType: KType,
    actualKlazz: KClass<*>,
    actualReturnType: KType
): Boolean {
    // If the expected function returns 'this' we allow the actual 'this' to be returned,
    // otherwise, the return types should be equal.
    if (expectedReturnType.classifier == expectedKlazz) {
        require(actualReturnType.classifier == actualKlazz) { "Expected return type to be $actualKlazz, found ${actualReturnType.classifier}" }
    } else {
        require(actualReturnType == expectedReturnType) { "Expected return type to be $expectedReturnType, found $actualReturnType" }
    }

    return true
}
