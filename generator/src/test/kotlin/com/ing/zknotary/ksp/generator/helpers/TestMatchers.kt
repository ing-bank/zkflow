package com.ing.zknotary.ksp.generator.helpers

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.reflection.shouldHaveMemberProperty
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

object TestMatchers {
    infix fun KClass<out Any>.shouldHaveSamePublicStructureWith(that: KClass<out Any>) {
        memberProperties.filter { property ->
            property.visibility?.let { it == KVisibility.PUBLIC }
                ?: error("Property $this.$property has undefined visibility")
        }.forEach {
            that shouldHaveMemberProperty it.name
        }
    }

    infix fun KClass<out Any>.shouldHaveConstructorsAlignedWith(that: KClass<out Any>) {
        require(constructors.any { it.parameters.isEmpty() }) {
            "${this.simpleName} must have an empty constructor"
        }

        require(constructors.any { it.parameters.size == that.memberProperties.size }) {
            "${this.simpleName} must have constructor with all public properties"
        }

        require(
            constructors.any {
                it.parameters.size == 1 &&
                    it.parameters.first().type.asTypeName() == that.asTypeName()
            }
        ) {
            "${this.simpleName} must be constructable from $that"
        }
    }

    infix fun KClass<out Any>.shouldBeAlignedWith(that: KClass<out Any>) {
        this shouldHaveSamePublicStructureWith that
        that shouldHaveSamePublicStructureWith this
        this shouldHaveConstructorsAlignedWith that
    }
}
