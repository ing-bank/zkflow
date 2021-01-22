package com.ing.zknotary.ksp.generator

import com.ing.zknotary.ksp.generator.fixtures.PublicKeyDefaultValue
import com.ing.zknotary.ksp.generator.helpers.KSPCompilationHelper.SimpleSource
import com.ing.zknotary.ksp.generator.helpers.KSPCompilationHelper.buildSizedVersion
import com.ing.zknotary.ksp.generator.helpers.TestMatchers.shouldBeAlignedWith
import com.ing.zknotary.ksp.generator.helpers.constructorFrom
import com.ing.zknotary.ksp.generator.helpers.get
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

class CustomTypesTest {
    private val publicKey: PublicKey

    init {
        val random = SecureRandom.getInstance("SHA1PRNG")
        val keygen = KeyPairGenerator.getInstance("DSA", "SUN")
        keygen.initialize(1024, random)
        publicKey = keygen.genKeyPair().public
    }

    @Test
    fun `class with custom sizeable types must be sizeable`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "StateL0",
                "@Sized class StateL0(val simple: @FixToLength(4) List<Int>)"
            ),
            SimpleSource(
                "StateL1",
                "@Sized class StateL1(val complex: @FixToLength(5) List<StateL0>)"
            )
        )

        val sourceClassL0 = loader.load("StateL0")
        val sourceClassL1 = loader.load("StateL1")
        val targetClass = loader.load("StateL1Sized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClassL1

        // Construction alignment.
        val sourceL0 = sourceClassL0.constructors.single().call(listOf(19))
        val sourceL1 = sourceClassL1.constructors.single().call(listOf(sourceL0))
        val target = targetClass.constructorFrom(sourceClassL1).call(sourceL1)

        target["complex"]["size"] shouldBe 5
        target["complex"]["originalSize"] shouldBe sourceL1["complex"]["size"]

        target["complex"]["0"]["simple"]["size"] shouldBe 4
        target["complex"]["0"]["simple"]["originalSize"] shouldBe
            sourceL1["complex"]["0"]["simple"]["size"]
    }

    @Test
    fun `class with custom defaultable types must be sizeable`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "Defaultable",
                """
                    class Defaultable(val n: Int) {
                        constructor() : this(0)
                    }
                """.trimMargin()
            ),
            SimpleSource(
                "ListWithDefault",
                """@Sized
                    class ListWithDefault(
                        val simple: @UseDefault Defaultable,
                        val shallow: @FixToLength(5) List<@UseDefault Defaultable>
                    )
                """.trimIndent()
            )
        )

        val defaultableClass = loader.load("Defaultable")
        val listWithDefaultClass = loader.load("ListWithDefault")
        val targetClass = loader.load("ListWithDefaultSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith listWithDefaultClass

        // Construction alignment.
        val defaultable = defaultableClass
            .constructors
            .single { it.parameters.isNotEmpty() }
            .call(2)

        val listWithDefault = listWithDefaultClass.constructors.single().call(defaultable, listOf(defaultable))

        val target = targetClass.constructorFrom(listWithDefaultClass).call(listWithDefault)

        target["shallow"]["size"] shouldBe 5
        target["shallow"]["originalSize"] shouldBe listWithDefault["shallow"]["size"]

        // Check elements we placed there.
        target["shallow"]["0"]["n"] shouldBe 2
        // Check a filler element created by default.
        target["shallow"]["1"]["n"] shouldBe 0
    }

    @Test
    fun `elements annotated with CallDefaultValueClass must be processed`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "CallDefaultValueClassState",
                """
                @Sized class CallDefaultValueClassState(
                    val pubKey: @CallDefaultValueClass(
                        "${PublicKeyDefaultValue::class.qualifiedName}"
                    ) ${PublicKey::class.qualifiedName}
                )
                """.trimIndent()
            )
        )

        val sourceClass = loader.load("CallDefaultValueClassState")
        val targetClass = loader.load("CallDefaultValueClassStateSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call(publicKey)
        val target = targetClass.constructorFrom(sourceClass).call(source)

        target["pubKey"] shouldBe source["pubKey"]
    }

    @Test
    fun `lists with elements annotated with CallDefaultValueClass must be processed`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "CallDefaultValueClassList",
                """
                @Sized class CallDefaultValueClassList(
                    val keys: @FixToLength(2) List<
                            @CallDefaultValueClass("${PublicKeyDefaultValue::class.qualifiedName}")
                            ${PublicKey::class.qualifiedName}
                        >                   
                )
                """.trimIndent()
            )
        )

        val sourceClass = loader.load("CallDefaultValueClassList")
        val targetClass = loader.load("CallDefaultValueClassListSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call(listOf(publicKey))
        val target = targetClass.constructorFrom(sourceClass).call(source)

        target["keys"]["size"] shouldBe 2
        target["keys"]["0"] shouldBe source["keys"]["0"]
        target["keys"]["1"] shouldBe PublicKeyDefaultValue().default
    }
}
