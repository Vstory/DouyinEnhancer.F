package io.github.twyora.douyinenhancer.utils.crypto

import java.io.InputStream
import java.security.MessageDigest

fun String.md5Hex(): String {
    val digest = MessageDigest.getInstance("MD5")
    return digest.digest(this.toByteArray(Charsets.UTF_8)).joinToString("") {
        "%02x".format(it)
    }
}

fun InputStream.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    this.use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read = input.read(buffer)
        while (read != -1) {
            digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
    }
    return digest.digest().joinToString("") {
        "%02x".format(it)
    }
}
