package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Товар в корзине")
data class CartItemResponse(
    @field:Schema(description = "ID товара", example = "2")
    val id: Int,

    @field:Schema(description = "Название товара", example = "Раф")
    val name: String,

    @field:Schema(description = "Выбранный размер / вариант", example = "L")
    val selectedSize: String,

    @field:Schema(description = "Цена за единицу", example = "345.9")
    val price: Float,

    @field:Schema(description = "Количество", example = "2")
    val quantity: Int,

    @field:Schema(description = "Общая стоимость", example = "691.8")
    val totalPrice: Float,

    @field:Schema(description = "Имя файла изображения", example = "raf.jpg")
    val imageName: String
)

@Schema(description = "Содержимое корзины")
data class CartSummaryResponse(
    @field:Schema(description = "Товары в корзине")
    val items: List<CartItemResponse>,

    @field:Schema(description = "Общее количество товаров", example = "5")
    val totalItems: Int,

    @field:Schema(description = "Общая стоимость", example = "1250.5")
    val totalPrice: Float
)
