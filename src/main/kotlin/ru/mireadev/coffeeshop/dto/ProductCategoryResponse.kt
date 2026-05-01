package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Категория товара")
data class ProductCategoryResponse(
    @field:Schema(description = "ID категории", example = "1")
    val id: Int,

    @field:Schema(description = "Название категории", example = "На молоке")
    val type: String
)
