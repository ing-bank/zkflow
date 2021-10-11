package com.ing.zkflow.compilation.zinc.template

fun interface TemplateLoader {
    fun loadTemplate(parameters: TemplateParameters): String
}
