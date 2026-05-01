package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Запрос на создание заказа")
data class OrderRequest(
    @field:Schema(description = "Адрес доставки", example = "ул. Пушкина, д. Колотушкина, кв. 10", required = true)
    val deliveryAddress: String,

    @field:Schema(description = "Стоимость доставки", example = "50.00", required = true)
    val deliveryFee: BigDecimal = BigDecimal.ZERO,

    @field:Schema(description = "Выбранные товары для заказа", required = true)
    val items: List<OrderCartItem>
)

@Schema(description = "Элемент корзины для заказа")
data class OrderCartItem(
    @field:Schema(description = "ID товара", example = "1", required = true)
    val productId: Int,

    @field:Schema(description = "Выбранный размер / вариант", example = "L", required = true)
    val selectedSize: String
)