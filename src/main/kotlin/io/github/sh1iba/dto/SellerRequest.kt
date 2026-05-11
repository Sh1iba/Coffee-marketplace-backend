package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание или обновление магазина продавца")
data class SellerRequest(

    @field:Schema(description = "Название магазина", example = "Urban Brew", required = true)
    @field:NotBlank @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Описание магазина", required = true)
    @field:NotBlank @field:Size(max = 500)
    val description: String,

    @field:Schema(description = "Категория магазина", example = "Кофейня", required = true)
    @field:NotBlank @field:Size(max = 50)
    val category: String,

    @field:Schema(description = "Контактный телефон", example = "+7 999 123-45-67", required = true)
    @field:NotBlank @field:Size(max = 20)
    val phone: String,

    @field:Schema(description = "URL сайта (необязательно)", example = "https://urbanbrew.ru")
    @field:Size(max = 200)
    val website: String? = null,

    @field:Schema(description = "URL логотипа магазина")
    val logoUrl: String? = null
)
