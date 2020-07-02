plugins {
    id("com.cosminpolifronie.gradle.plantuml") version "1.6.0"
    id("org.danilopianini.gradle-latex") version "0.2.5"
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
    // Process all tex files in the docs dir
    projectDir.walk().maxDepth(1).filter { it.isFile && it.extension == "tex" }.forEach {texFile ->
        texFile.name() {
            extraArguments = listOf(
                "-file-line-error",
                "-output-format=pdf",
                "-output-directory=build",
                "-aux-directory=.",
                "-shell-escape",
                "-synctex=1",
                "-interaction=nonstopmode",
                "-halt-on-error",
                "-quiet"
            )
        }

    }
}
