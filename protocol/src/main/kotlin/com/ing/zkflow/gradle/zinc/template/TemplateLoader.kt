package com.ing.zkflow.gradle.zinc.template

fun interface TemplateLoader {
    fun loadTemplate(parameters: TemplateParameters): String
}
