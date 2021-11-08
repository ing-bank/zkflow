package com.ing.zkflow.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldBeEmptyDirectory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

internal class PathsTest {
    @Test
    fun `ensureFile must create a new file if it does not exist yet`(@TempDir tempDir: Path) {
        val actual = tempDir.ensureFile(FOO)
        actual shouldBe tempDir.resolve(FOO)
        actual.shouldBeAFile()
    }

    @Test
    fun `ensureFile must return the file if it already exists`(@TempDir tempDir: Path) {
        Files.createFile(tempDir.resolve(FOO))

        val actual = tempDir.ensureFile(FOO)
        actual.shouldBeAFile()
    }

    @Test
    fun `ensureFile must fail when a directory exists with the required name`(@TempDir tempDir: Path) {
        Files.createDirectory(tempDir.resolve(FOO))

        val ex = shouldThrow<IllegalArgumentException> {
            tempDir.ensureFile(FOO)
        }
        ex.message shouldStartWith "Not a regular file: "
    }

    @Test
    fun `ensureDirectory must create a new directory if it does not exist yet`(@TempDir tempDir: Path) {
        val actual = tempDir.ensureDirectory(FOO)
        actual shouldBe tempDir.resolve(FOO)
        actual.shouldBeEmptyDirectory()
    }

    @Test
    fun `ensureDirectory must return the directory if it already exists`(@TempDir tempDir: Path) {
        Files.createDirectory(tempDir.resolve(FOO))

        val actual = tempDir.ensureDirectory(FOO)
        actual.shouldBeADirectory()
    }

    @Test
    fun `ensureDirectory must fail when a file exists with the required name`(@TempDir tempDir: Path) {
        Files.createFile(tempDir.resolve(FOO))

        val ex = shouldThrow<IllegalArgumentException> {
            tempDir.ensureDirectory(FOO)
        }
        ex.message shouldStartWith "Not a directory: "
    }

    companion object {
        const val FOO = "foo"
    }
}
