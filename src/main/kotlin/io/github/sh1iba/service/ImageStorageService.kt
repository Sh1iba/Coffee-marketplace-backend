package io.github.sh1iba.service

import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@Service
class ImageStorageService(
    @Value("\${cloudinary.cloud-name}") cloudName: String,
    @Value("\${cloudinary.api-key}") apiKey: String,
    @Value("\${cloudinary.api-secret}") apiSecret: String
) {
    private val cloudinary = Cloudinary(
        mapOf("cloud_name" to cloudName, "api_key" to apiKey, "api_secret" to apiSecret)
    )

    private val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")

    fun saveImage(file: MultipartFile): String {
        if (file.isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        val contentType = file.contentType ?: ""
        if (contentType !in allowedTypes) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, GIF, WebP are allowed")
        }
        return uploadBytes(file.bytes)
    }

    fun saveBytes(bytes: ByteArray, publicId: String): String =
        uploadBytes(bytes, publicId)

    private fun uploadBytes(bytes: ByteArray, publicId: String? = null): String {
        val options = mutableMapOf<String, Any>("folder" to "coffee-marketplace")
        if (publicId != null) {
            options["public_id"] = publicId
            options["overwrite"] = false
        }
        val result = cloudinary.uploader().upload(bytes, options)
        return result["secure_url"] as String
    }

    fun deleteByUrl(url: String, usageCount: Long) {
        if (usageCount > 0) return
        val publicId = url
            .substringAfter("/upload/")
            .let { if (it.contains("/")) it else it }
            .substringBeforeLast(".")
        runCatching { cloudinary.uploader().destroy(publicId, emptyMap<String, Any>()) }
    }
}
