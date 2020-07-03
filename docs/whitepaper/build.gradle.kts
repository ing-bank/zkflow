plugins {
    id("org.danilopianini.gradle-latex")
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
    )

    val watchFiles = fileTree(projectDir) { include("**/*.tex") }.toList()

    // Process all tex files in the docs dir
    projectDir.walk().maxDepth(1)
        .filter { it.isFile && it.extension == "tex" }
        .forEach { texFile ->
            texFile.name() {
                extraArguments = pdfLatexArgs
                watching = watchFiles

            }
        }
}
