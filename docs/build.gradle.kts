plugins {
    id("com.cosminpolifronie.gradle.plantuml")
    id("org.danilopianini.gradle-latex")
}

plantUml {
    render(object {
        val input = "**/*.puml"
        val output = "build"
        val format = "png"
        val withMetadata = false
    })
}

latex {
    val pdfLatexArgs = listOf(
        "-file-line-error",
        "-output-format=pdf",
        "-output-directory=build",
        "-aux-directory=.",
        "-shell-escape",
        "-synctex=1",
        "-interaction=nonstopmode",
        "-halt-on-error"
        // "-quiet"
    )
    // Process all tex files in the docs dir
    projectDir.walk().maxDepth(1)
        .filter { it.isFile && it.extension == "tex" }
        .forEach { texFile ->
            texFile.name() { extraArguments = pdfLatexArgs }
        }
}
