package com.ing.zknotary.gradle.template

fun interface TemplateLoader {
    fun loadTemplate(parameters: TemplateParameters): String
}
