import com.ing.zkflow.gradle.extension.ZKFlowExtension
import com.ing.zkflow.gradle.task.joinConstFiles
import com.ing.zkflow.gradle.zinc.template.TemplateConfigurations
import com.ing.zkflow.gradle.zinc.template.TemplateConfigurations.Companion.doubleTemplateParameters
import com.ing.zkflow.gradle.zinc.template.TemplateConfigurations.Companion.floatTemplateParameters
import com.ing.zkflow.gradle.zinc.template.TemplateParameters
import com.ing.zkflow.gradle.zinc.template.TemplateRenderer
import com.ing.zkflow.gradle.zinc.template.parameters.AbstractPartyTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.AbstractPartyTemplateParameters.Companion.ANONYMOUS_PARTY_TYPE_NAME
import com.ing.zkflow.gradle.zinc.template.parameters.AmountTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.BigDecimalTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.CollectionTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.IntegerTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.IssuedTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.MapTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.PublicKeyTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.SerializedStateTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.SignersTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.StateGroupTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.StringTemplateParameters
import com.ing.zkflow.gradle.zinc.template.parameters.TxStateTemplateParameters
import com.ing.zkflow.gradle.zinc.util.CircuitConfigurator
import com.ing.zkflow.gradle.zinc.util.CodeGenerator
import com.ing.zkflow.gradle.zinc.util.MerkleReplacer
import com.ing.zkflow.gradle.zinc.util.ZincSourcesCopier
import net.corda.core.crypto.Crypto
import java.io.File

val templateConfigurations = getTemplateConfiguration()

fun main(args: Array<String>) {
    val root = args[0]
    val projectVersion = args[1]

    // Render templates for circuits testing deserialization, etc.
    TemplateRenderer(getPlatformSourcesTestSourcesPath(root).toPath()) {
        getTemplateContents(root, it.templateFile)
    }.apply {
        templateConfigurations.resolveAllTemplateParameters().forEach(::renderTemplate)
    }

    val circuitSourcesBase = File("$root/circuits")
    val statesPath = "states"
    val circuitStates = circuitSourcesBase.resolve(statesPath)
    val mergedCircuitOutput = File("$root/build/circuits")

    circuitSourcesBase
        .listFiles { dir, file ->
            dir.resolve(file).isDirectory && file != statesPath
        }
        ?.map { it.name }
        ?.forEach { circuitName ->
            val outputPath = mergedCircuitOutput.resolve(circuitName).resolve("src")
            val circuitSourcesPath = circuitSourcesBase.resolve(circuitName)

            // Read the configuration
            val configurator = CircuitConfigurator.fromSources(circuitSourcesPath, ZKFlowExtension.CONFIG_CIRCUIT_FILE)
            configurator.generateConstsFile(outputPath)

            // Copy Zinc sources
            val copier = ZincSourcesCopier(outputPath)
            copier.copyZincCircuitSources(circuitSourcesPath, circuitName, projectVersion, ZKFlowExtension.CONFIG_CIRCUIT_FILE)
            copier.copyZincCircuitStates(getCircuitStates(circuitStates, configurator.circuitConfiguration.circuit.states))
            copier.copyZincPlatformSources(getPlatformSources(root))
            copier.copyZincPlatformSources(getPlatformLibs(root))

            val consts = joinConstFiles(outputPath, getPlatformSourcesPath(root))

            // Render templates
            val templateRenderer = TemplateRenderer(outputPath.toPath()) {
                getTemplateContents(root, it.templateFile)
            }

            val templateConfigurationsForCircuit = getTemplateConfiguration()
            templateConfigurationsForCircuit.apply {
                configurator.circuitConfiguration.circuit.states.forEach { state ->
                    // Existence of the required states is ensured during the copying.
                    addConfigurations(TxStateTemplateParameters(state))
                }

                addConfigurations(SignersTemplateParameters(configurator.circuitConfiguration.groups.signerGroup))
            }
                .resolveAllTemplateParameters()
                .forEach(templateRenderer::renderTemplate)

            // Render multi-state templates
            renderStateTemplates(configurator, templateRenderer, templateConfigurationsForCircuit)

            // Generate code
            val codeGenerator = CodeGenerator(outputPath)
            getTemplateContents(root, "merkle_template.zn").also { codeGenerator.generateMerkleUtilsCode(it, consts) }
            getTemplateContents(root, "main_template.zn").also { codeGenerator.generateMainCode(it, consts) }

            // Replace placeholders in Merkle tree functions
            val replacer = MerkleReplacer(outputPath)
            replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
            replacer.setCorrespondingMerkleTreeFunctionForMainTree()
        }
}

private fun getPlatformSourcesPath(root: String): File {
    return File("$root/src/main/resources/zinc-platform-sources")
}

private fun getCircuitStates(circuitStates: File, states: List<CircuitConfigurator.State>): List<File> {
    return states.map { state ->
        val module = circuitStates.resolve(state.location)
        require(module.exists()) { "Expected ${module.absolutePath}" }
        module
    }
}

private fun getPlatformSources(root: String): Array<File>? {
    return File("$root/src/main/resources/zinc-platform-sources").listFiles()
}

private fun getPlatformLibs(root: String): Array<File>? {
    return File("$root/src/main/resources/zinc-platform-libraries").listFiles()
}

private fun getPlatformSourcesTestSourcesPath(root: String): File {
    return File("$root/build/zinc-platform-test-sources")
}

private fun getTemplateContents(root: String, templateName: String) =
    runCatching {
        File("$root/src/main/resources/zinc-platform-templates").listFiles() ?: error("Templates must be accessible")
    }
        .mapCatching { templates ->
            templates.single { it.name == templateName } ?: error("Multiple templates for $templateName found")
        }
        .map { it.readText() }
        .getOrThrow()

private fun renderStateTemplates(configurator: CircuitConfigurator, templateRenderer: TemplateRenderer, templateConfigurationsForCircuit: TemplateConfigurations) {
    templateConfigurationsForCircuit.apply {
        configurator.circuitConfiguration.groups.inputGroup.filter { it.stateGroupSize > 0 }.forEach { stateGroup ->
            addConfigurations(SerializedStateTemplateParameters("input", stateGroup))
        }
        addConfigurations(StateGroupTemplateParameters("input", configurator.circuitConfiguration.groups.inputGroup))

        configurator.circuitConfiguration.groups.outputGroup.filter { it.stateGroupSize > 0 }.forEach { stateGroup ->
            addConfigurations(SerializedStateTemplateParameters("output", stateGroup))
        }
        addConfigurations(StateGroupTemplateParameters("output", configurator.circuitConfiguration.groups.outputGroup))

        configurator.circuitConfiguration.groups.referenceGroup.filter { it.stateGroupSize > 0 }.forEach { stateGroup ->
            addConfigurations(SerializedStateTemplateParameters("reference", stateGroup))
        }
        addConfigurations(
            StateGroupTemplateParameters(
                "reference",
                configurator.circuitConfiguration.groups.referenceGroup
            )
        )
    }.resolveAllTemplateParameters()
        .forEach(templateRenderer::renderTemplate)
}

private fun getTemplateConfiguration(): TemplateConfigurations {
    return TemplateConfigurations().apply {
        // BigDecimal configurations
        val bigDecimalTemplateParameters = listOf(
            BigDecimalTemplateParameters(24, 6),
            BigDecimalTemplateParameters(100, 20),
            floatTemplateParameters,
            doubleTemplateParameters,
        )
        addConfigurations(bigDecimalTemplateParameters)

        // Amount configurations
        val amountTemplateParameters = bigDecimalTemplateParameters.map { AmountTemplateParameters(it, 8) }
        addConfigurations(amountTemplateParameters)

        // String configurations
        addConfigurations(StringTemplateParameters(32))

        // Issued configurations
        addConfigurations(
            IssuedTemplateParameters(
                AbstractPartyTemplateParameters.selectAbstractPartyParameters(Crypto.EDDSA_ED25519_SHA512.schemeCodeName),
                StringTemplateParameters(1)
            )
        )

        addConfigurations(
            CollectionTemplateParameters(collectionSize = 3, innerTemplateParameters = StringTemplateParameters(1)),
            CollectionTemplateParameters<TemplateParameters>(
                "collection_integer.zn",
                collectionSize = 3,
                platformModuleName = "u32"
            ),
            CollectionTemplateParameters<TemplateParameters>(
                "collection_integer.zn",
                collectionSize = 2,
                platformModuleName = "i32"
            )
        )

        // Collection of participants to TestState.
        addConfigurations(
            CollectionTemplateParameters(
                collectionSize = 2,
                innerTemplateParameters = AbstractPartyTemplateParameters(
                    ANONYMOUS_PARTY_TYPE_NAME,
                    PublicKeyTemplateParameters.eddsaTemplateParameters
                )
            )
        )

        addConfigurations(
            MapTemplateParameters(
                "StringToIntMap",
                6,
                StringTemplateParameters(5),
                IntegerTemplateParameters.i32
            )
        )
    }
}
