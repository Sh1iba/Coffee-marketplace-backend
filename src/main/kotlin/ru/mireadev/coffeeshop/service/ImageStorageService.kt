package ru.mireadev.coffeeshop.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class ImageStorageService(
    @Value("\${app.images.directory}") private val imagesDirectory: String
) {
    private val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")

    init {
        Files.createDirectories(Paths.get(imagesDirectory))
    }

    fun saveImage(file: MultipartFile): String {
        if (file.isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")

        val contentType = file.contentType ?: ""
        if (contentType !in allowedTypes) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, GIF, WebP are allowed")
        }

        val extension = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() } ?: "jpg"

        val fileName = "${UUID.randomUUID()}.$extension"
        val target = Paths.get(imagesDirectory, fileName)
        Files.copy(file.inputStream, target)
        return fileName
    }

    fun getImageResource(fileName: String): Resource {
        val filePath = Paths.get(imagesDirectory, fileName)
        val resource = UrlResource(filePath.toUri())
        if (resource.exists() && resource.isReadable) return resource
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found: $fileName")
    }
}
