package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema
import ru.mireadev.coffeeshop.entity.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "Информация о заказе")
data class OrderResponse(
    @field:Schema(description = "ID заказа", example = "1")
    val id: Long,

    @field:Schema(description = "Общая сумма заказа", example = "1250.50")
    val totalAmount: BigDecimal,

    @field:Schema(description = "Стоимость доставки", example = "50.00")
    val deliveryFee: BigDecimal,

    @field:Schema(description = "Адрес доставки", example = "ул. Пушкина, д. 10, кв. 5")
    val deliveryAddress: String,

    @field:Schema(description = "Дата заказа")
    val orderDate: LocalDateTime,

    @field:Schema(description = "Статус заказа", example = "PENDING")
    val status: OrderStatus,

    @field:Schema(description = "Элементы заказа")
    val items: List<OrderItemResponse>
)

@Schema(description = "Элемент заказа")
data class OrderItemResponse(
    @field:Schema(description = "ID элемента", example = "1")
    val id: Long,

    @field:Schema(description = "Название товара", example = "Раф")
    val productName: String,

    @field:Schema(description = "Выбранный вариант", example = "L")
    val selectedSize: String,

    @field:Schema(description = "Цена за единицу", example = "345.90")
    val unitPrice: BigDecimal,

    @field:Schema(description = "Количество", example = "2")
    val quantity: Int,

    @field:Schema(description = "Общая стоимость элемента", example = "691.80")
    val totalPrice: BigDecimal
)
