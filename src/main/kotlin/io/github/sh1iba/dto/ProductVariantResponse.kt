package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Вариант товара (размер и цена)")
data class ProductVariantResponse(
    @field:Schema(description = "Размер / вариант", example = "L")
    val size: String,

    @field:Schema(description = "Цена", example = "345.9")
    val price: Float,

    @field:Schema(description = "Объём или вес, например '250 мл' или '300 г'", example = "250 мл")
    val volume: String? = null
)
