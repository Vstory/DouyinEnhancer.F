package io.github.twyora.douyinenhancer.utils.crypto

import java.security.MessageDigest

fun String.md5Hex(): String {
    val digest = MessageDigest.getInstance("MD5")
    return digest.digest(this.toByteArray(Charsets.UTF_8)).joinToString("") {
        "%02x".format(it)
    }
}
