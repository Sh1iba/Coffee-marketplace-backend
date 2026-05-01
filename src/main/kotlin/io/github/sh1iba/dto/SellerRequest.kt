package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание или обновление магазина продавца")
data class SellerRequest(

    @field:Schema(description = "Название магазина", example = "Coffee House", required = true)
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Описание магазина", example = "Лучший кофе в городе", required = true)
    @field:NotBlank
    @field:Size(max = 500)
    val description: String,

    @field:Schema(description = "Категория магазина", example = "Кофейня", required = true)
    @field:NotBlank
    @field:Size(max = 50)
    val category: String,

    @field:Schema(description = "Имя файла логотипа", example = "logo_123.png", required = false)
    val logoImage: String? = null
)
