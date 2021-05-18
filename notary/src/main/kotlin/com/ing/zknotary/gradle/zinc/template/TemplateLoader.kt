package com.ing.zknotary.gradle.zinc.template

fun interface TemplateLoader {
    fun loadTemplate(parameters: TemplateParameters): String
}
