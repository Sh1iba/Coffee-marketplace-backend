package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Товар в избранном")
data class FavoriteProductResponse(
    @field:Schema(description = "ID товара", example = "2")
    val id: Int,

    @field:Schema(description = "Выбранный размер / вариант", example = "L")
    val selectedSize: String
)
