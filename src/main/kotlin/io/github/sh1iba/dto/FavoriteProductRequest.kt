package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос на добавление товара в избранное")
data class FavoriteProductRequest(
    @field:Schema(description = "ID товара", example = "1", required = true)
    val productId: Int,

    @field:Schema(description = "Выбранный размер / вариант", example = "L", required = true)
    val selectedSize: String
)
