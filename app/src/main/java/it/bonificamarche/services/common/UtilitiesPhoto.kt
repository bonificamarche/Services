package it.bonificamarche.services.common

import java.io.File
import java.util.*

private val TAG = "Utilities Photo"

/**
 * Find photos to send to the server.
 * @return number of photos to send.
 */
fun findPhotoToSend(appName: String): Int {

    val imagesDir = File("/storage/emulated/0/Pictures/${appName}/")
    var counterImg = 0

    show(TAG, "Search photo in ${imagesDir.path}")
    if (imagesDir.exists() && imagesDir.isDirectory) {
        show("Fun find photo", "Search photos...")
        imagesDir.listFiles()?.forEach { _ ->
            counterImg += 1
        }
    }
    return counterImg
}

/**
 * Encode photo in base64.
 * @param path: photo to encoded.
 */
fun encodePhoto(path: String): String {
    val bytes = File(path).readBytes()
    return Base64.getEncoder().encodeToString(bytes)
}
