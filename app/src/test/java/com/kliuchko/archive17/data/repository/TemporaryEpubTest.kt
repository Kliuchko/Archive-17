package com.kliuchko.archive17.data.repository

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryEpubTest {
    @Test
    fun `accepts zip signature used by epub files`() {
        withTemporaryFile(byteArrayOf(0x50, 0x4B, 0x03, 0x04)) { file ->
            assertTrue(file.hasEpubSignature())
        }
    }

    @Test
    fun `rejects non epub response body`() {
        withTemporaryFile("<html>Unavailable</html>".toByteArray()) { file ->
            assertFalse(file.hasEpubSignature())
        }
    }

    private fun withTemporaryFile(bytes: ByteArray, block: (File) -> Unit) {
        val file = File.createTempFile("archive17-epub-test", ".tmp")
        try {
            file.writeBytes(bytes)
            block(file)
        } finally {
            file.delete()
        }
    }
}
