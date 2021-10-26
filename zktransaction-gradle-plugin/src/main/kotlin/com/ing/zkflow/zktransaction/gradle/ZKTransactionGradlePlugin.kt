package com.ing.zkflow.zktransaction.gradle

import com.ing.zkflow.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ZKTransactionGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        // NOP
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project: Project = kotlinCompilation.target.project
        return project.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = BuildConfig.zktransactionPluginName

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.zktransactionPluginGroupId,
        artifactId = BuildConfig.zktransactionPluginArtifactId,
        version = BuildConfig.zktransactionPluginVersion,
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.target.project.plugins.hasPlugin(ZKTransactionGradlePlugin::class.java)
}
