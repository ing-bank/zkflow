// plugins {
//     id("com.cosminpolifronie.gradle.plantuml")
//     id("org.danilopianini.gradle-latex")
// }
//
// plantUml {
//     val diagrams = fileTree(projectDir) { include("**/*.puml") }.toList()
//
//     diagrams.map { it.relativeTo(projectDir) }.forEach { file ->
//         render(object {
//             val input = file.path
//             val output = file.parent + "/" + file.nameWithoutExtension + ".svg"
//             val format = "svg"
//             val withMetadata = true
//         })
//     }
//
//     tasks.named("plantUml") {
//         // outputs.upToDateWhen { false }
//     }
// }
//
// task("docs") {
//     dependsOn("buildLatex")
// }
//
// latex {
//     val pdfLatexArgs = listOf(
//         "-file-line-error",
//         "-output-format=pdf",
//         "-output-directory=build",
//         "-aux-directory=.",
//         "-shell-escape",
//         "-synctex=1",
//         "-interaction=nonstopmode",
//         "-halt-on-error"
//     )
//
//     val watchFiles = fileTree(projectDir) { include("**/*.tex") }.toList()
//
//     // Process all tex files in the docs dir
//     projectDir.walk().maxDepth(1)
//         .filter { it.isFile && it.extension == "tex" }
//         .forEach { texFile ->
//             texFile.name() {
//                 extraArguments = pdfLatexArgs
//                 watching = watchFiles
//
//             }
//         }
//
//     tasks.matching { it.name.contains("pdfLatex") }.forEach {
//         // it.outputs.upToDateWhen { false }
//         it.mustRunAfter("plantUml")
//         it.dependsOn("plantUml")
//
//         // also retrigger this task when plantUml diagrams change
//         val plantUmlFiles = tasks.named("plantUml").get().inputs.files
//         it.inputs.files(plantUmlFiles)
//     }
// }
