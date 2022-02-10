package com.ing.zkflow.compilation

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.File

fun joinConstFiles(circuitPath: File, platformSourcesPath: File): String {
    return circuitPath.resolve("consts.zn").readText() + "\n\n" +
        platformSourcesPath.resolve("platform_consts.zn").readText()
}

@SuppressFBWarnings("PATH_TRAVERSAL_IN", justification = "WONTFIX: will be thrown away anyway soon for ZincPoet")
fun String.folderIfExists(basePath: File): String? = takeIf { File(basePath.path + "/$this").exists() }
