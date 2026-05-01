package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос на добавление товара в корзину")
data class CartItemRequest(
    @field:Schema(description = "ID товара", example = "1", required = true)
    val productId: Int,

    @field:Schema(description = "Выбранный размер / вариант", example = "L", required = true)
    val selectedSize: String,

    @field:Schema(description = "Количество", example = "2", defaultValue = "1")
    val quantity: Int = 1
)

@Schema(description = "Запрос на обновление количества товара в корзине")
data class UpdateCartQuantityRequest(
    @field:Schema(description = "Новое количество", example = "3", required = true)
    val quantity: Int
)
